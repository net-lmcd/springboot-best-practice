package com.kts.out.imageserver.redisTest;

import com.kts.out.imageserver.data.avatar.ResponseAvatar;
import com.kt.narle.imageserver.redis.RedisStorageService;
import com.kts.out.imageserver.APIHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class redisTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RedisStorageService redisStorageService;

    @Autowired
    private JedisConnectionFactory jedisConnectionFactory;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private APIHelper apiHelper;

    @Before
    public void setUp(){
        this.redisStorageService.init();
    }

    @After
    public void setAfter() {
        this.redisStorageService.init();
    }

    @Test
    public void getAuth() {
        String xServerId = "269387c1-2ad4-42c6-87a9-25c7cbca4769";
        String xAuthTimestamp = "2019037324052410169";
        String xAuthSignature = "aed1ba2890ff504ef44e008850929648f088bef62e4d7d370bcea91edce5e70f";
    }
/*
    @Test
    public void saveImageResourceFromRedis() {
        RedisTemplate<String, Object> temp = this.redisTemplate;
        temp.setValueSerializer(new Jackson2JsonRedisSerializer<>(ResourceSaved.class));
        String uuid = "0b920356-095e-45b7-8d65-e9dbf6a0e686";

        List<Resource> resourceList = new ArrayList<>();
        Resource resource = new Resource("test_uuid", "test_filename", defaultResponse());
        resourceList.add(resource);

        ResourceSaved resourceSaved = ResourceSaved.builder()
                                                        .filepath("test/path")
                                                        .resources(resourceList)
                                                        .build();
        assertThat(resourceList.size()).isEqualTo(1);
        assertThat(resourceList.get(0)).isInstanceOf(Resource.class);

        temp.opsForValue().set(uuid, resourceSaved);
        ResourceSaved resourceFromRedis = (ResourceSaved)temp.opsForValue().get(uuid);

        assertThat(resourceFromRedis).isNotNull();
        assertThat(resourceFromRedis.getFilepath()).isEqualTo("test/path");
        assertThat(resourceFromRedis.getResources().size()).isEqualTo(1);

        temp.delete(uuid);
    }

    @Test
    public void durationTestForSaveImageResource() {
        RedisTemplate<String, Object> temp = this.redisTemplate;
        temp.setValueSerializer(new Jackson2JsonRedisSerializer<>(ResourceSaved.class));
        String uuid = "0b920356-095e-45b7-8d65-e9dbf6a0e686";
        ResponseAvatar responseAvatar = defaultResponse();

        Resource resource = Resource.builder()
                                    .uuid("0b920356-095e-45b7-8d65-e9dbf6a0e686")
                                    .filename("test_filename")
                                    .responseAvatar(responseAvatar)
                                    .build();

        List<Resource> resources = Collections.singletonList(resource);
        ResourceSaved resourceSaved = ResourceSaved.builder()
                                                            .filepath("test/dir")
                                                            .resources(resources)
                                                            .build();
        temp.opsForValue().set("uuid:" + uuid, resourceSaved, Duration.ofMillis(100));

        try {
            Thread.sleep(101);
        }catch(Exception e){
            logger.error(e.getMessage());
        }
        Object object = temp.opsForValue().get("uuid:"+uuid);
        assertThat(object).isNull();
        temp.delete("uuid:" + uuid);
    }
*/
    @Test
    public void zsetTest() {
        jedisConnectionFactory.getConnection().openPipeline();
        IntStream.range(0, 10).forEach(idx -> {
            jedisConnectionFactory.getConnection().zSetCommands().zAdd("test:".getBytes(), 10, ("test_value:" + idx).getBytes());
        });
        jedisConnectionFactory.getConnection().closePipeline();

        Set<byte[]> bytes = jedisConnectionFactory.getConnection().zSetCommands().zRangeByScore("test:".getBytes(),5,15,0,5);
        assertThat(bytes).isNotNull();
        assertThat(bytes.size()).isEqualTo(5);

        jedisConnectionFactory.getConnection().openPipeline();
        IntStream.range(0, 10).forEach(idx -> {
            jedisConnectionFactory.getConnection().del("test:".getBytes());
        });
        jedisConnectionFactory.getConnection().closePipeline();
    }

    @Test
    public void countServer() {
        String value =  "test_value_test";
        String value2 = "test_value2_test";
        jedisConnectionFactory.getConnection().openPipeline();
        jedisConnectionFactory.getConnection().zSetCommands().zAdd("test:".getBytes(), 0, value.getBytes());
        jedisConnectionFactory.getConnection().zSetCommands().zIncrBy("test:".getBytes(),1, value.getBytes());
        jedisConnectionFactory.getConnection().zSetCommands().zIncrBy("test:".getBytes(),2, value.getBytes());
        jedisConnectionFactory.getConnection().zSetCommands().zIncrBy("test:".getBytes(),3, value.getBytes());

        jedisConnectionFactory.getConnection().zSetCommands().zIncrBy("test:".getBytes(),4, value2.getBytes());
        jedisConnectionFactory.getConnection().zSetCommands().zIncrBy("test:".getBytes(),5, value2.getBytes());
        jedisConnectionFactory.getConnection().zSetCommands().zIncrBy("test:".getBytes(),6, value2.getBytes());
        jedisConnectionFactory.getConnection().closePipeline();

        Set<byte[]> bytes = jedisConnectionFactory.getConnection().zSetCommands().zRange("test:".getBytes(),0,0);
        assertThat(bytes.size()).isEqualTo(1);
        byte[] result = bytes.iterator().next();
        assertThat(result).isEqualTo(value.getBytes());

        jedisConnectionFactory.getConnection().del("test:".getBytes());
    }

    private static ResponseAvatar defaultResponse () {
        return ResponseAvatar.builder()
                        .filename("test_filename")
                        .gender(0)
                        .glassShape(0)
                        .glassType(0)
                        .menHairLength(1)
                        .menHairPart(1)
                        .menHairWave(1)
                        .womenHairFront(2)
                        .womenHairLength(2)
                        .womenHairWave(2)
                        .build();
    }
}
