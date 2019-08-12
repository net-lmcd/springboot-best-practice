package com.kts.out.imageserver.exception;

import com.kts.out.imageserver.utils.ErrorConst;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class RequestFullException extends RuntimeException {
    static final String message = ErrorConst.FULL_REQUEST_EXCEPTION;
    public RequestFullException() {
        super(message);
    }
}
