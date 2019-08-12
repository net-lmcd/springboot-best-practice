package com.kts.out.imageserver.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder @Getter @Setter
public class ErrorResponse {

    @JsonProperty("timestamp")
    private long timeStamp;
    @JsonProperty("status")
    private int statusCode;
    @JsonProperty("error")
    private String error;
    @JsonProperty("message")
    private String message;

    @JsonCreator
    public ErrorResponse(
            @JsonProperty("timestamp") long timeStamp,
            @JsonProperty("status") int statusCode,
            @JsonProperty("error") String httpError,
            @JsonProperty("message") String errorMessage) {
        this.timeStamp  = timeStamp;
        this.statusCode = statusCode;
        this.error = httpError;
        this.message = errorMessage;
    }
}
