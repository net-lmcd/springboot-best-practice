package com.kts.out.imageserver.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.out.imageserver.data.Auth;
import com.kts.out.imageserver.data.avatar.Resource;
import com.kts.out.imageserver.data.avatar.ResourceSaved;
import com.kts.out.imageserver.exception.RedisFileNotFoundException;
import com.kts.out.imageserver.exception.RedisOperationException;
import com.kts.out.imageserver.exception.RequestFullException;
import com.kts.out.imageserver.exception.UnAuthorizationException;
import com.kts.out.imageserver.utils.ErrorConst;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

/** Description Redis -ZSET
 *
 * Sorted Sets는 key 하나에 여러개의 score와 value로 구성됩니다.
 * Value는 score로 sort되며 중복되지 않습니다.
 * score가 같으면 value로 sort됩니다.
 * Sorted Sets에서는 집합이라는 의미에서 value를 member라 부릅니다.
 *
 * If a specified member is already a member of the sorted set,
 * the score is updated and the element is reinserted at the right position to ensure correct ordering.
 * If the key does not exist, a new sorted set with the specified members as sole members is created,
 * like if the sorted set was empty.
 *
 * ZADD options (Redis 3.0.2 or greater)
 * ZADD supports a list of options, specified after the name of the key and before the first score argument. Options are:
 *
 * XX: Only update elements that already exist. Never add elements.
 * NX: Don't update already existing elements. Always add new elements
 * INCR: When this option is specified ZADD acts like ZINCRBY. Only one score-element pair can be specified in this mode.
 */

@Slf4j
@Service
public class RedisStorageService {
    private static final String UUID = "uuid";
    private static final String SERVER = "server";
    @Value("${server.list.atc}")
    private String atcServerList;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    RedisScript<Object> script;

    @PostConstruct
    public void init() {
        whenInitBean();
    }

    /**
     * DEFAULT AUTH - 초기화
     */
    public void saveDefaultAuth() {
        setAuth("269387c1-2ad4-42c6-87a9-25c7cbca4769", new Auth("image-server", "p@assW@ord"));
    }

    /**
     * AUTH SETTING
     * @param uuid uuid
     * @param auth Auth
     */
    public void setAuth(String uuid, Auth auth) {
        try {
            String auth_string = objectMapper.writeValueAsString(auth);
            redisTemplate.opsForValue().setIfAbsent(uuid, auth_string);
        } catch(JsonProcessingException e) {
            log.error("[REDIS] setAuth ERROR => : " + e.getMessage());
            throw new RedisOperationException("Redis ObjectMapper Read Value - Deserialization Error");
        }
    }

    /**
     * GET AUTH
     * @param xServerKey xServerId from httpHeader
     * @return Auth
     */
    public Auth getAuth(String xServerKey) {
        String auth_string = (String) Optional.ofNullable(redisTemplate.opsForValue().get(xServerKey))
                .orElseThrow(() -> new UnAuthorizationException(ErrorConst.AUTH_FAILED_EXCEPTION));
        try{
            return objectMapper.readValue(auth_string, Auth.class);
        } catch(IOException e) {
            log.error("[REDIS] getAuth ERROR => : " + e.getMessage());
            throw new RedisOperationException("Redis ObjectMapper Read Value - Deserialization Error");
        }
    }

    /**
     * uuid 로 분석 결과 get
     * @param uuid uuid
     * @return String From Redis
     */
    private String getResourcesByUUID(String uuid) {
        return (String) Optional.ofNullable(redisTemplate.opsForValue().get(uuid))
                .orElseThrow(() -> new RedisFileNotFoundException(ErrorConst.NO_RESOURCE_EXCEPTION, uuid));
    }

    /**
     * string -> ResourceSaved (obj) deserialize
     * @param uuid uuid
     * @return ResourceSaved
     */
    private ResourceSaved resourceSerialize(String uuid) {
        try {
            String resource = getResourcesByUUID(uuid);
            return objectMapper.readValue(resource, ResourceSaved.class);
        } catch(IOException e) {
            log.error("[REDIS] getResourceSaved ERROR => : " + e.getMessage());
            throw new RedisOperationException("Redis ObjectMapper Read Value - Deserialization Error");
        }
    }

    /**
     *
     * @param uuid uuid
     * @return ResponseAvatar
     */
    public Map <String, Object> getResource(String uuid) {
        Map <String, Object> responseAvatar = null;
        ResourceSaved resourceSaved = resourceSerialize(uuid);

        List<Resource> resources = resourceSaved.getResources();
        for (int idx = 0; idx <= resources.size(); idx++) {
            Resource resource = resources.get(idx);
            if (resource.getUuid().equals(uuid)) {
                responseAvatar = resource.getResponseAvatar();
                break;
            }
        }
        return responseAvatar;
    }

    /**
     * T type object serializing -> json string
     * @param t t
     * @param <T> T
     * @return String
     */
    private <T> String writeJsonValueAsString(T t)  {
        try {
            return objectMapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            log.error("[REDIS] writeJsonValueAsString ERROR => " + e.getMessage());
            throw new RedisOperationException("writeJsonValueAsString Failed");
        }
    }

    /**
     * Set Value If Absent
     * @param uuid uuid
     * @param resourceSaved resource For Save
     * @return boolean
     */
    private Boolean setValueIfAbsent(String uuid, String resourceSaved) {
        return redisTemplate.opsForValue().setIfAbsent(UUID + ":" + uuid, resourceSaved, Duration.ofDays(1));
    }

    /**
     * zSet의 value가 0 ~ 5사이의 값중, 가장 작은것부터 1개 추출 (min, max, offset, count)
     * @param key key
     * @return Set
     */
    private Set<?> zRangeByScore(String key) {
        return redisTemplate.opsForZSet().rangeByScore(key, 0, 4, 0, 1);
    }

    /**
     * 최소 Count Server 구한다.
     * @return "address:port"
     */
    public String getMinKeyFromSortedSet() { //zrangebyscore server 0 5 withscores limit 0 1, zrange server 0 0 withscores
        Set<?> objects =  zRangeByScore(SERVER + ":"); //min, max, offset, count
        return (String) Optional.ofNullable(objects)
                                .orElseThrow(RequestFullException::new)
                                .stream()
                                .findFirst().orElseThrow(RequestFullException::new);
    }

    /**
     * Server Count +1 / -1
     * @param httpMethod GET/POST
     * @param value ser:ip
     */
    public void setServerCount(HttpMethod httpMethod, String value) {
        switch (httpMethod) { // atomic operations
            case GET:
                redisTemplate.execute((RedisCallback<Double>) connection ->
                        connection.zIncrBy((SERVER + ":").getBytes(), -1 , value.getBytes()));
                break;
            case POST:
                redisTemplate.execute((RedisCallback<Double>) connection ->
                        connection.zIncrBy((SERVER + ":").getBytes(), 1, value.getBytes()));
                break;
            default:
                break;
        }
    }

    /**
     * 카운트가 작은 서버 address:port get - request 보낼 수 있는 서버
     * @return String spare server
     */
    @SuppressWarnings("unchecked")
    public String whichSpareServer() {
        List<String> spares = null;
        try {
            spares = (ArrayList <String>) redisTemplate.execute(script, Collections.singletonList(SERVER +":"), "0", "4", "0", "1"); //min, max, offset, count
            return Optional.ofNullable(spares)
                           .orElseThrow(RequestFullException::new) // if spares null -> exception
                           .stream()
                           .findFirst()
                           .orElseThrow(RequestFullException::new); // if findFirst(isEmpty) null -> exception
        } catch (Exception e) {
            log.error("[REDIS] RequestFullException [SPARE SERVER] ==> NO SPARE SERVER");
            throw new RequestFullException();
        }
    }

    /**
     * saveResponseAndSpareCount - save Image and set server address count
     * @param httpMethod GET
     * @param spareServer from whichSpareServer()
     * @param filepath filename
     * @param responseBody Map - response Body From AvatarExtract
     * @param uuid String uuid
     * @return true/false
     */
    public boolean saveResponseAndCountSpare(HttpMethod httpMethod, String spareServer, String filepath, Map <String, Object> responseBody/*ResponseAvatar responseAvatar*/, String uuid) {
        ResourceSaved resourceSaved = saveImage(filepath, responseBody, uuid);
        Boolean executed = redisTemplate.execute((RedisCallback<Boolean>) connection -> {
            connection.openPipeline(); //open pipeline
            if (HttpMethod.GET == httpMethod) connection.zIncrBy((SERVER + ":").getBytes(), -1 , spareServer.getBytes()); //GET -> count -1
            connection.setNX(uuid.getBytes(), writeJsonValueAsString(resourceSaved).getBytes()); // set received data from Avatar Analytics Server
            return connection.closePipeline() //close pipeline & return
                    .stream()
                    .anyMatch(ele -> ele.equals(Boolean.FALSE));
        });
        return (boolean) executed;
    }

    /**
     * SAVE IMAGE ANALYTIC RESPONSE TO REDIS - IMAGE 분석 결과 저장
     * @param filename filename
     * @param responseBody Avatar 분석 서버로부터의 결과
     * @param uuid 고유 uuid
     * @return uuid
     */
    public ResourceSaved saveImage(String filename, Map <String, Object> responseBody, String uuid) {
        String path = "upload-dir/" + filename;
        Resource resource = makeResource(uuid,filename, responseBody);
        return ResourceSaved.builder()
                .filepath(path)
                .resources(Collections.singletonList(resource))
                .build();
    }

    /**
     * SAVE IMAGE TO REDIS- Redis Connection Factory - NO USE
     * @param uuid uuid
     * @param contentEncoded byte[], image buffer
     */
    public void saveImage(String uuid, byte[] contentEncoded) {
        try {
            jedisConnectionFactory.getConnection().openPipeline();
            jedisConnectionFactory.getConnection().setNX((uuid).getBytes(), contentEncoded);
            jedisConnectionFactory.getConnection().expire((uuid).getBytes(), 3600);
            jedisConnectionFactory.getConnection().closePipeline();
        } catch(Exception e) {
            log.error("saveImage : " + e.getMessage());
        }
    }

    /**
     * Resource 생성
     * @param uuid  String
     * @param filename String
     * @param responseAvatar Map <String, Object>
     * @return Resource
     */
    private Resource makeResource(String uuid, String filename, Map <String, Object> responseAvatar) {
        return Resource.builder()
                .uuid(uuid)
                .filename(filename)
                .responseAvatar(responseAvatar)
                .build();
    }

    /**
     * 빈 초기화 시점 PostConstruct
     */
    private void whenInitBean() {
        List<String> serverList = atcServerList();
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.openPipeline();
            serverList.forEach(server -> connection.zSetCommands().zAdd ((SERVER + ":").getBytes(), 0, server.getBytes()));
            return connection.closePipeline();
        });
        saveDefaultAuth();
    }

    /**
     * yml 로 부터 atc serer list 읽어옴.
     * @return List
     */
    private List<String> atcServerList() {
        List<String> serverList = new ArrayList<>();
        atcServerList = atcServerList.trim();
        atcServerList = atcServerList.replaceAll(" ","");
        atcServerList = atcServerList.replaceAll("\\p{Z}", "");

        StringTokenizer st = new StringTokenizer(atcServerList,",");
        while(st.hasMoreTokens())
            serverList.add(st.nextToken());
        return serverList;
    }
}



