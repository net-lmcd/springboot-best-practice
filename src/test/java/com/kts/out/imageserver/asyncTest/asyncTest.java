package com.kts.out.imageserver.asyncTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.out.imageserver.storage.FileSystemStorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.util.Map;


@RunWith(SpringRunner.class)
@SpringBootTest
public class asyncTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileSystemStorageService fileSystemStorageService;

    @Test
    public void asyncTest() {};

    public Map<String, Object> responseJsonToMap(String response) {
        Map<String, Object> responseMap = null;
        try{
            responseMap = objectMapper.readValue(response, Map.class);
        }catch(IOException e){
            throw new RuntimeException(e.getMessage());
        }
        return responseMap;
    }
}
