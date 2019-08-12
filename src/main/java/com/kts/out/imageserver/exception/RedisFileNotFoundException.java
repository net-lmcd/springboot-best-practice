package com.kts.out.imageserver.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.NOT_FOUND)
public class RedisFileNotFoundException extends RuntimeException{
    private String uuid;
    public RedisFileNotFoundException(String message, String uuid) {
        super(message);
        this.uuid = uuid;
    }
}
