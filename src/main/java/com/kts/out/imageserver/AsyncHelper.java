package com.kts.out.imageserver;

import com.kts.out.imageserver.exception.AvatarExtractServerErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class AsyncHelper {
    @Autowired
    private RestTemplate restTemplate;

    /**
     * httpResponseEntity for Supplier => CompletableFuture.supplyAsync(supplier)
     * Supplier<ResponseEntity <? extends Response>> supplier = () -> httpResponseEntity(responseEntity);
     * @param responseEntity
     * @return
     */
    private <T> ResponseEntity<T> httpResponseEntity(ResponseEntity<T> responseEntity) { //for supplier
        return responseEntity;
    }

    /**
     * request to Avatar Analytics Server
     * thread pool - asyncExecutor
     * @param url
     * @param requestBody
     * @return
     */
    @Async("asyncExecutor")
    public CompletableFuture<ResponseEntity <String>> requestFaceAttributeAnalyticsFutureAsync(String url, HttpEntity<? extends Map> requestBody) {
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange("http://" + url + "/upload", HttpMethod.POST, requestBody, String.class);
            return CompletableFuture.supplyAsync(() -> httpResponseEntity(responseEntity)); //supplier
        } catch (Exception e) {
            throw new AvatarExtractServerErrorException();
        }
    }
}
