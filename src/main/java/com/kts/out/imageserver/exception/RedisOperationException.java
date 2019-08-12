package com.kts.out.imageserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RedisOperationException extends RuntimeException{
    public RedisOperationException(String message) {
        super(message);
    }
}
