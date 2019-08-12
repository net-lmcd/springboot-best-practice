package com.kts.out.imageserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ZookeeperNotFoundException extends RuntimeException {
    public ZookeeperNotFoundException(String message) {
        super(message);
    }
}
