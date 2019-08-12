package com.kts.out.imageserver.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.out.imageserver.exception.AvatarExtractServerErrorException;
import com.kts.out.imageserver.exception.AvatarGenerateServerErrorException;
import com.kts.out.imageserver.redis.RedisStorageService;
import com.kts.out.imageserver.utils.APIUtils;
import com.kts.out.imageserver.AsyncHelper;
import com.kts.out.imageserver.storage.FileSystemStorageService;
import com.kts.out.imageserver.APIHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** Description

 *TODO <1> (완료)
 *   1. 이 이미지를 쏴주는 컨트롤러는 요청이 들어오면, 주키퍼에 접근해 path에서 데이터를 가져온다. (완료) -> Zookeeper 사용 X
 *   2. 읽어온 데이터 중에서 number가 가장 작은 키를 꺼낸다.(완료)
 *
 *TODO <3> (완료)
 *   2. ATC server에 전송할 리퀘스트 객체 생성. (완료)
 *
 *TODO <3-1> (완료)
 *   1. Redis number - 1 -> SetRedis(POST, key(uri), path) (주키퍼 제외)
 *   2. restTemplate http Call 실행.(완료)
 *   2. server Ip는 TO.DO<2> 에서 받아온 키, 이미지를 POST하고 response를 받는다. (완료)
 *
 *TODO <4> Response가 왔다면, Redis write, 이 서버 입장에서는 GET 한것. (완료)
 *   1. 보낼때는 POST number + 1 SetRedis(POST, key(url), path)
 *   2. 받을때는 GET number - 1 SetRedis(GET, key(url), path)
 */
@Slf4j
@RestController
@CrossOrigin("*")
public class ImageController {
    @Autowired
    private RedisStorageService redisStorageService;
    @Autowired
    private APIHelper apiHelper;
    @Autowired
    private AsyncHelper asyncHelper;
    @Autowired
    private FileSystemStorageService fileSystemStorageService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Qualifier("multipart-header")
    @Autowired
    private HttpHeaders multiPartHttpHeader;
    @Qualifier("application-json-header")
    @Autowired
    private HttpHeaders applicationJsonHttpHeader;

    @PostConstruct
    public void init() {
        fileSystemStorageService.init();
    }

    /**
     * 고유 uuid 를 통해 분석 결과 GET
     * @param servletRequest (자동 매핑)
     * @param uuid (http param)
     * @return ResponseEntity <?>
     */
    @GetMapping("/faceAvatarExtract/{uuid}")
    public ResponseEntity<?> getFaceExtractDataByUUID(HttpServletRequest servletRequest, @Valid @PathVariable("uuid") String uuid) {
        //apiHelper.checkAuth(servletRequest);
        Map <String, Object> responseAvatar = redisStorageService.getResource(uuid);
        return ResponseEntity.ok(responseAvatar);
    }

    /**
     * faceAvatarExtract API - tomcat min/max thread 30 / 500 - 비동기 처리 필요 없다고 판단됨.
     * @param servletRequest request
     * @param image multipart
     * @return responseEntity

     */
    @PostMapping("/faceAvatarExtract")
    public ResponseEntity<?> faceAttributeExtract(HttpServletRequest servletRequest, @NotBlank @RequestParam("image")MultipartFile image) {
        log.info("[REST CONTROLLER] /faceAttributeExtract => [RequestParam] MultipartFile image [filename] : " + image.getOriginalFilename() + " image [size] : " + image.getSize());
        apiHelper.checkAuth(servletRequest, image.getOriginalFilename());
        UUID uuid = UUID.randomUUID();
        String filePath = apiHelper.makePath(uuid, image.getOriginalFilename());
        LinkedList <Object> extractValueList = APIUtils.addElement(image.getResource(), uuid);

        fileSystemStorageService.store(filePath,image); //image 저장.

        MultiValueMap<String, Object> body = apiHelper.addElementToLinkedMultiMap(APIUtils.extractKeyList, extractValueList);
        HttpEntity <MultiValueMap> httpEntity = apiHelper.createHttpMultiValueMapEntity(body, multiPartHttpHeader);
        String spareServer = redisStorageService.whichSpareServer(); // COUNT +1

        try {
            log.info("[REST CONTROLLER] faceAvatarExtract() REQUEST SEND"); // response body too long -> just status
            ResponseEntity<String> response = restTemplate.exchange(APIUtils.HTTP + spareServer + APIUtils.FACE_EXTRACT_PATH, HttpMethod.POST, httpEntity, String.class);
            Map <String, Object> responseBody = apiHelper.responseJsonStringToMap(response.getBody());
            log.info("[REST CONTROLLER] faceAvatarExtract() RESPONSE : " +  objectMapper.writeValueAsString(responseBody));

            if (HttpStatus.OK == response.getStatusCode()) {
                redisStorageService.saveResponseAndCountSpare(HttpMethod.GET, spareServer, filePath, responseBody, uuid.toString()); // COUNT -1
                URI location = ServletUriComponentsBuilder
                            .fromCurrentServletMapping()
                            .path("faceAvatarExtract/{uuid}")
                            .build()
                            .expand(uuid)
                            .toUri();

                return ResponseEntity.created(location)
                            .headers(applicationJsonHttpHeader)
                            .body(responseBody);
            }else {
                log.error("[REST CONTROLLER] faceAvatarExtract() ATC SERVER ERROR => " + "[STATUS] => " + response.getStatusCodeValue() + ", [CAUSE] => " + response.getBody());
                throw new AvatarExtractServerErrorException();
            }
        } catch (Exception e) { //catch Exception
            redisStorageService.setServerCount(HttpMethod.GET, spareServer);
            log.error("[REST CONTROLLER] faceAvatarExtract() ATC SERVER ERROR  " +  "[CAUSE] => " + e.getMessage());
            throw new AvatarExtractServerErrorException();
        }
    }

    /**
     * face Avatar Generate API tomcat min/max thread 30 / 500 - 비동기 처리 필요 없다고 판단됨.
     * @param servletRequest servletRequest
     * @param data multipart/form-data "data"
     * @param value multipart/form-data "params"
     * @return
     */
    @PostMapping("/faceAvatarGenerate")
    public ResponseEntity<Map> faceAvatarGenerate(HttpServletRequest servletRequest, @NotBlank @RequestParam("data") MultipartFile data, @NotBlank @RequestParam("params") String value) {
        log.info("[REST CONTROLLER] /faceAvatarGenerate => [RequestParam] MultipartFile obj [filename] : " + data.getOriginalFilename() + " ,obj [size] : " + data.getSize() + " / [RequestParam] String [normal_value] : " + value);
        apiHelper.checkAuth(servletRequest, data.getOriginalFilename());
        LinkedList <Object> generateValueList = APIUtils.addElement(data.getResource(), value);

        MultiValueMap <String, Object> body = apiHelper.addElementToLinkedMultiMap(APIUtils.generateKeyList, generateValueList);
        HttpEntity <MultiValueMap> httpEntity = apiHelper.createHttpMultiValueMapEntity(body, multiPartHttpHeader);

        try {
            log.info("[REST CONTROLLER]] faceAvatarGenerate() REQUEST SEND "); // response body too long -> just status
            ResponseEntity<String> response = restTemplate.exchange(APIUtils.HTTP + APIUtils.FACE_GENERATE_ADDRESS_DEV + APIUtils.FACE_GENERATE_PATH, HttpMethod.POST, httpEntity, String.class);
            log.info("[REST CONTROLLER]] faceAvatarGenerate() RESPONSE STATUS : " +  response.getStatusCode()); // response body too long -> just status
            return ResponseEntity.ok(apiHelper.responseJsonStringToMap(response.getBody()));
        } catch (Exception e) {
            log.error("[REST CONTROLLER]] faceAvatarNormal() ERROR " + "[CAUSE] => " + e.getMessage());
            throw new AvatarGenerateServerErrorException();
        }
    }


    /**
     * Refactoring TEST API !!!!
     * @param servletRequest servletRequest
     * @param image muitipart/form-data
     * @return responseEntity
     */
    @PostMapping("/faceAvatarExtract/test")
    public ResponseEntity <?> faceAttributeExtract2(HttpServletRequest servletRequest, @RequestParam("image") MultipartFile image) {
        apiHelper.checkAuth(servletRequest, image.getOriginalFilename());

        UUID uuid = UUID.randomUUID();
        String filePath = apiHelper.makePath(uuid, image.getOriginalFilename());
        String minCountServerAddress = redisStorageService.getMinKeyFromSortedSet(); //routing할 분석 모델 server ip
        String url = apiHelper.getRequestURI(minCountServerAddress);

        fileSystemStorageService.store(filePath, image); //image 저장.

        // TODO -> refactoring중 .. 위 코드 helper class 활용해 아래 2줄로 변경
        MultiValueMap <String, Object> body = apiHelper.createLinkedMultiMapRequest(image.getResource(), uuid);
        HttpEntity <MultiValueMap> httpEntity = apiHelper.createHttpMultiValueMapEntity(body, multiPartHttpHeader);

        redisStorageService.setServerCount(HttpMethod.POST, minCountServerAddress); //post -> 해당 서버 ip count + 1
        ResponseEntity<String> response = null;
        Map <String, Object> responseBody = null;
        try {
            response = asyncHelper.requestFaceAttributeAnalyticsFutureAsync(url, httpEntity).get(); // post & get Response
            responseBody = apiHelper.responseJsonStringToMap(response.getBody());
        } catch (InterruptedException | ExecutionException e) {
            throw new AvatarExtractServerErrorException();
        }

        if (HttpStatus.OK == response.getStatusCode()) {
            redisStorageService.setServerCount(HttpMethod.GET, minCountServerAddress); //count - 1
            redisStorageService.saveImage(filePath, responseBody, uuid.toString());
            URI location = ServletUriComponentsBuilder.fromCurrentServletMapping()
                        .path("faceAvatarExtract/{uuid}")
                        .build()
                        .expand(uuid)
                        .toUri();

            return ResponseEntity.created(location)
                        .headers(applicationJsonHttpHeader)
                        .body(responseBody);
        }else {
            log.error("faceAvatarExtract Response Status is not completed, HttpStatus = " + response.getStatusCodeValue() + "\n" + "cause = " + responseBody);
            redisStorageService.setServerCount(HttpMethod.GET, minCountServerAddress);
            throw new AvatarExtractServerErrorException();
        }
    }

    /**
     * Test API
     * @return
     */
    /*
    @GetMapping("/auth")
    public String makeServerIdSecret() {
        try {
            this.redisStorageService.saveDefaultAuth();
        }catch(Exception e) {
            logger.info("Auth Controller Error :" + e.getMessage());
        }
        return "created";
    }*/

}

