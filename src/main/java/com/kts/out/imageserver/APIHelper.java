package com.kts.out.imageserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.out.imageserver.data.Auth;
import com.kts.out.imageserver.exception.FileNotSupportedException;
import com.kts.out.imageserver.exception.UnAuthorizationException;
import com.kts.out.imageserver.redis.RedisStorageService;
import com.kts.out.imageserver.utils.ErrorConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@Slf4j
@Component
public class APIHelper {
    @Autowired
    private RedisStorageService redisStorageService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TypeReference typeReference;

    /**
     * make auth key
     * @param timestamp timestamp
     * @param id id
     * @param secret secret
     * @return str
     */
    private String makeKey(String timestamp, String id, String secret) {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec((secret).getBytes("UTF-8"), "HmacsSHA256");

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            String digestTarget = id + "." +timestamp;
            byte[] bytes = mac.doFinal(digestTarget.getBytes("ASCII"));
            StringBuffer hash = new StringBuffer();
            for(int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if(hex.length() == 1) hash.append('0');
                hash.append(hex);
            }
            digest = hash.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return digest;
    }

    /**
     * auth and file type check
     * @param servletRequest request
     * @param filename filename
     */
    public void checkAuth(HttpServletRequest servletRequest, String filename) {
        checkAuth(servletRequest);
        checkFileType(filename);
    }

    /**
     * auth check
     * @param servletRequest servletRequest
     */
    private void checkAuth(@NotNull HttpServletRequest servletRequest) {
        String xServerKey = servletRequest.getHeader("x-server-key");
        String timestamp = servletRequest.getHeader("x-auth-timestamp");
        String xAuthSignature = servletRequest.getHeader("x-auth-signature");
        if (xServerKey == null || timestamp == null || xAuthSignature == null) {
            log.error("[CHECK AUTH] [x-server-key] => " + xServerKey + " / [x-auth-timestamp] => " + timestamp +" / [x-auth-signature] => " + xAuthSignature);
            throw new UnAuthorizationException(ErrorConst.AUTH_FAILED_EXCEPTION);
        }
        Auth auth = redisStorageService.getAuth(xServerKey);
        String keys = makeKey(timestamp, xServerKey, auth.getPassword());

        if (!keys.equals(xAuthSignature))
            throw new UnAuthorizationException(ErrorConst.AUTH_FAILED_EXCEPTION);
    }

    /**
     * fileType Check
     * @param filename filename
     */
    private void checkFileType(String filename) {
        String fileType = filename.toLowerCase();
        if (!(fileType.endsWith(".png") || fileType.endsWith(".jpg") || fileType.endsWith("jpeg") || fileType.endsWith("obj"))) {
            log.error("[API HELPER] checkFileType() ERROR => " + ErrorConst.NOT_SUPPORTED_FILE);
            throw new FileNotSupportedException(ErrorConst.NOT_SUPPORTED_FILE);
        }
    }

    /**
     * failure response
     * @return
     */
    public Map <String, Object> failureFromServer() {
        return responseJsonStringToMap("{\"result\":\"현재 응답을 받을 수 없습니다. 잠시후에 다시 시도해주세요.\"}");
    }

    /**
     * no output response
     * @return
     */
    public Map <String, Object> noResultFromServer() {
        return responseJsonStringToMap("{\"result\":\"분석 결과가 없습니다.\"}");
    }

    /**
     * response Json To Map
     * @param response str
     * @return map
     */
    public Map <String, Object> responseJsonStringToMap(String response) {
        try{
            //response = response.replaceAll("^\"|\"$|\\\\", "");
            Map <String, Object> map = objectMapper.readValue(response, typeReference);
            return map;
        }catch(Exception e){
            throw new RuntimeException("responseJsonToMap: " + e.getMessage());
        }
    }

    /**
     * make path
     * @param uuid uuid
     * @param filename f name
     * @return str
     */
    public String makePath(UUID uuid, String filename) {
        return uuid + "$" + StringUtils.cleanPath(filename);
    }

    /**
     * URI 생성 - no use
     * @param minCountServerAddress
     * @return
     */
    public String getRequestURI(String minCountServerAddress) {
        try {
            return new URI("http://" + minCountServerAddress + "/upload").toString();
        }catch(URISyntaxException e) {
            throw new RuntimeException("URI Syntax Error : " + minCountServerAddress);
        }
    }

    /**
     * face attribute 분석 server로 넘길 request 생성 (유저별)
     * @param image_resource 리소스
     * @param value uuid
     * @return MultiValueMap <String, V>
     */
    public <T> MultiValueMap <String, T> createLinkedMultiMapRequest(T image_resource, T value) {
        LinkedMultiValueMap <String, T> multiValueMap = new LinkedMultiValueMap<>();
        addRequestLinkedMultiMap("image", image_resource, multiValueMap); //call by reference
        addRequestLinkedMultiMap("userId", value, multiValueMap); //call by reference
        return multiValueMap;
    }

    /**
     * Map(body), Header => HttpEntity // header 없는경우 / 있는경우 매핑
     * @param bodyMap httpBody
     * @param headers httpHeaders (optional)
     * @param <K> K
     * @param <V> V
     * @return
     */
    private <K, V> HttpEntity <? extends Map> createHttpMapEntity(Map <K, V> bodyMap, Optional<HttpHeaders> headers) {
        return headers.map(h -> new HttpEntity<>(bodyMap, h))
                      .orElseGet(() -> new HttpEntity<>(bodyMap));
    }

    /**
     * @param bodyMap httpBody
     * @param headers httpHeaders (NotNull)
     * @param <K> K
     * @param <V> V
     * @return HttpEntity <MultiValueMap> (type cast) - uncheck
     */
    @SuppressWarnings("unchecked")
    public <K, V> HttpEntity <MultiValueMap> createHttpMultiValueMapEntity(Map <K, V> bodyMap, @NotNull HttpHeaders headers) {
        return (HttpEntity <MultiValueMap>) createHttpMapEntity(bodyMap, Optional.of(headers));
    }

    /** key, value ArrayList / LinkedList 분리, KeyList 는 static (계속 get, O(1)), ValueList 는 request 마다 생성 -> poll.
     * insert, length 체크 (다르게 구현해도 무방)
     * @param keys key list
     * @param values value list
     * @param <K> K
     * @param <V> V
     * @return MultiValueMap or null
     */
    public <K, V> MultiValueMap <K, V> addElementToLinkedMultiMap(ArrayList <? extends K> keys,  LinkedList <? extends V> values) { // instance 생산 -> extends
        LinkedMultiValueMap <K, V> multiValueMap = new LinkedMultiValueMap<>();
        int size = (keys.size() == values.size()) ? keys.size() : 0;
        int idx = 0;
        while (idx < size) {
            K key = keys.get(idx);
            V value = values.poll();
            addRequestLinkedMultiMap(key, value, multiValueMap);
            idx ++;
        }
        return  multiValueMap;
    }

    /**
     * insert key / value, call by reference, LinkedMultiValueMap - NotNull
     * @param key key
     * @param value value
     * @param linkedMultiValueMap NotNull
     * @param <K> K
     * @param <V> V
     */
    private <K, V> void addRequestLinkedMultiMap(@NotNull K key, @NotNull V value, @NotNull LinkedMultiValueMap <K, V> linkedMultiValueMap) {
        linkedMultiValueMap.add(key, value);
    }

    /**
     * insert atomic (key / value), call by reference, LinkedMultiValueMap - Optional(Nullable)
     * @param key key
     * @param value value
     * @param linkedMultiValueMapOptional optional
     * @param <K> K
     * @param <V> V
     */
    private <K, V> MultiValueMap <K, V> addAtomicRequestLinkedMultiMapOptional(@NotNull K key, @NotNull V value, Optional <LinkedMultiValueMap <K, V>> linkedMultiValueMapOptional) {
        MultiValueMap <K, V> linkedMultiValueMapReference = linkedMultiValueMapOptional.orElseGet(LinkedMultiValueMap::new); //null -> new LinkedMultiMap
        linkedMultiValueMapReference.add(key, value);
        return linkedMultiValueMapReference;
    }
}
