package com.kts.out.imageserver.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnAuthorizationException extends RuntimeException{
    public UnAuthorizationException(String message) {
        super(message);
    }
}
