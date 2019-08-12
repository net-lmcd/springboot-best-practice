package com.kts.out.imageserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FileNotSupportedException extends RuntimeException {
    public FileNotSupportedException(String message) {
        super(message);
    }
}
