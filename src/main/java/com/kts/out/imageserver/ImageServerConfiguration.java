package com.kts.out.imageserver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kts.out.imageserver.utils.NObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import redis.clients.jedis.JedisPoolConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

@Slf4j
@EnableAsync
@Configuration
public class ImageServerConfiguration {

    private static final NObjectMapper OBJECT_MAPPER = new NObjectMapper();

    @Value("${task.max-thread:200}")
    private int maxThreadPoolCount;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private Integer redisPort;

    @Value("${spring.redis.password}")
    private String redisPassword;

    @Value("${tomcat.ajp.protocol}")
    private String ajpProtocol;

    @Value("${tomcat.ajp.port}")
    private int ajpPort;

    @Value("${tomcat.ajp.enabled}")
    boolean tomcatAjpEnabled;

    @Value("${redis.sentinel.master}")
    String sentinel_master;

    @Value("${redis.sentinel.first}")
    String sentinel_first;

    @Value("${redis.sentinel.second}")
    String sentinel_second;

    @Value("${redis.sentinel.third}")
    String sentinel_third;

    @Value("${redis.sentinel.port}")
    int sentinel_port;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(200);
        poolConfig.setMaxIdle(200);
        poolConfig.setMinIdle(12);
        poolConfig.setMaxWaitMillis(30);
        return poolConfig;
    }

    @Bean("redis-connection")
    @Profile("local")
    public RedisConnectionFactory redisConnectionFactoryLocal() {
        log.info("[BEAN INIT] RedisConnectionFactory, master => " + sentinel_master + "[first] => " + sentinel_first + " [second] => " + sentinel_second + " [third] => " + sentinel_third);
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration() //sentinel config
                .master(sentinel_master)
                .sentinel(sentinel_first, 26379)
                .sentinel(sentinel_second, 26380)
                .sentinel(sentinel_third, 26381);
        sentinelConfig.setPassword(redisPassword);
        sentinelConfig.setDatabase(0);

        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfigurationBuilder = JedisClientConfiguration.builder();
        jedisClientConfigurationBuilder.connectTimeout(Duration.ofSeconds(30));
        jedisClientConfigurationBuilder.usePooling().poolConfig(jedisPoolConfig());
        return new JedisConnectionFactory(sentinelConfig, jedisClientConfigurationBuilder.build());
    }

    @Bean
    @Profile("dev")
    public JedisConnectionFactory jedisConnectionFactory() {
        // Tip ->> With Spring Data Redis 2.0 JedisConnectionFactory.set*() is deprecated!!!  use RedisStandaloneConfiguration
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setPort(redisPort);
        redisStandaloneConfiguration.setPassword(redisPassword);
        redisStandaloneConfiguration.setDatabase(0);

        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfigurationBuilder = JedisClientConfiguration.builder();
        jedisClientConfigurationBuilder.connectTimeout(Duration.ofSeconds(30));
        jedisClientConfigurationBuilder.usePooling().poolConfig(jedisPoolConfig());
        return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfigurationBuilder.build());
    }

    /**
     * Sentinel Support Configuration
     * @return
     */
    @Bean("redis-connection")
    @Profile({"prod"})
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("[BEAN INIT] RedisConnectionFactory, master => " + sentinel_master + "[first] => " + sentinel_first + " [second] => " + sentinel_second + " [third] => " + sentinel_third);
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration() //sentinel config
                .master(sentinel_master)
                .sentinel(sentinel_first, sentinel_port)
                .sentinel(sentinel_second, sentinel_port)
                .sentinel(sentinel_third, sentinel_port);
        sentinelConfig.setPassword(redisPassword);
        sentinelConfig.setDatabase(0);

        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfigurationBuilder = JedisClientConfiguration.builder();
        jedisClientConfigurationBuilder.connectTimeout(Duration.ofSeconds(30));
        jedisClientConfigurationBuilder.usePooling().poolConfig(jedisPoolConfig());
        return new JedisConnectionFactory(sentinelConfig, jedisClientConfigurationBuilder.build());
    }

    @Bean
    @Profile("prod")
    public RedisTemplate<String, Object> redisTemplate() {
        final RedisTemplate<String, Object> template = createTemplate(); // new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setEnableTransactionSupport(true);
        return template;
    }

    @Bean
    @Profile("dev")
    public RedisTemplate<String, Object> redisTemplateDev() {
        final RedisTemplate<String, Object> template = createTemplate(); // = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setEnableTransactionSupport(true);
        return template;
    }

    @Bean
    @Profile("local")
    public RedisTemplate<String, Object> redisTemplateLocal() {
        final RedisTemplate<String, Object> template = createTemplate(); // = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactoryLocal());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.setEnableTransactionSupport(true);
        return template;
    }

    @SuppressWarnings("unchecked")
    @Bean
    public RedisScript<Object> script() throws IOException {
        String location = "/app/redis/";
        try {
            Path rootLocation = Paths.get(location);
            Path file = rootLocation.resolve("script.lua");
            Resource resource = new UrlResource(file.toUri());
            ScriptSource scriptSource = new ResourceScriptSource(resource);
            return RedisScript.of(scriptSource.getScriptAsString(), Object.class);
        } catch (MalformedURLException e) {
            log.error("[CONFIGURATION] REDIS LUA SCRIPT SET FAILED => " + e.getMessage());
            throw new FileNotFoundException();
        }
    }

    /**
     * TaskThreadPool 생성
     *
     * @return TaskThreadPool
     */
    @Bean("asyncExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30);
        executor.setMaxPoolSize(maxThreadPoolCount);
        executor.setQueueCapacity(10);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * ObjectMapper Bean
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper messageObjectMapper() {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        return OBJECT_MAPPER;
    }

    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }


    /**
     * Apache - Embedded Tomcat - mod_jk 이용시 connect 해야함. mod_proxy에선 필요 X
     * @return
    */
    @Profile("prod")
    @Bean
    public ServletWebServerFactory serverFactory() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        tomcat.addAdditionalTomcatConnectors(createAjpConnection());
        return tomcat;
    }

    private Connector createAjpConnection() {
        Connector ajpConnector = new Connector(ajpProtocol);
        ajpConnector.setPort(ajpPort);
        ajpConnector.setSecure(false);
        ajpConnector.setScheme("http");
        ajpConnector.setAllowTrace(true);
        return ajpConnector;
    }

    /**
     * Jackson Convert Bean
     * @param objectMapper Jackson Convert
     * @return Convert
     */
    @Bean
    public MappingJackson2HttpMessageConverter mappingJacksonHttpMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    @Bean
    public TypeReference typeReference() {
        return new TypeReference<Map<String, Object>>() {};
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Bean(name = "multipart-header")
    public HttpHeaders multipartContentHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    @Bean(name = "application-json-header")
    public HttpHeaders applicationJsonHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Bean(name = "test-header")
    public HttpHeaders defaultHeader() {
        HttpHeaders header = new HttpHeaders();
        header.set("x-server-key", "269387c1-2ad4-42c6-87a9-25c7cbca4769");
        header.set("x-auth-timestamp", "2019037324052410169");
        header.set("x-auth-signature", "aed1ba2890ff504ef44e008850929648f088bef62e4d7d370bcea91edce5e70f");
        return header;
    }

    private RedisTemplate<String, Object> createTemplate() { return new RedisTemplate<>(); }
}
