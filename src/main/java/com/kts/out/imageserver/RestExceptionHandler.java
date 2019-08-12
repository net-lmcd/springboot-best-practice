package com.kts.out.imageserver;

import com.kts.out.imageserver.data.ErrorResponse;
import com.kts.out.imageserver.exception.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestController
@RestControllerAdvice
public class RestExceptionHandler
{

    @ExceptionHandler(value = RedisOperationException.class)
    protected ResponseEntity<ErrorResponse> handleRedisOperationException(RedisOperationException re) {
        ErrorResponse errorResponse = getErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, re.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(value = RedisFileNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleFileNotFoundException(RedisFileNotFoundException re) {
        ErrorResponse errorResponse = getErrorResponse(HttpStatus.NOT_FOUND, re.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(value = ZookeeperNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleFileNotFoundException(ZookeeperNotFoundException re) {
        ErrorResponse errorResponse = getErrorResponse(HttpStatus.NOT_FOUND, re.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler({AvatarExtractServerErrorException.class, RequestFullException.class, ZKException.class})
    protected ResponseEntity<ErrorResponse> handleServerException(Exception re) {
        ErrorResponse errorResponse = getErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, re.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(value = FileNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handlerFileNotSupportedException(FileNotSupportedException re) {
        ErrorResponse errorResponse = getErrorResponse(HttpStatus.BAD_REQUEST, re.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(value = AvatarGenerateServerErrorException.class)
    protected ResponseEntity<ErrorResponse> handleNormalServerErrorException(AvatarGenerateServerErrorException re) {
        ErrorResponse errorResponse = getErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, re.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private ErrorResponse getErrorResponse(HttpStatus httpStatus, String message) {
        return ErrorResponse.builder()
                         .timeStamp(makeTimeStamp())
                         .statusCode(httpStatus.value())
                         .error(httpStatus.getReasonPhrase())
                         .message(message)
                         .build();
    }

    private long makeTimeStamp() {
        return new Date().getTime();
    }
}