package com.example.demo.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private boolean success;
    private String code;
    private int status;
    private String message;
    private Map<String, String> errors;
    private LocalDateTime timestamp;
    private String path;

    public static ErrorResponse of(String code, int status, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    public static ErrorResponse of(String code, int status, String message,
                                   Map<String, String> errors, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .status(status)
                .message(message)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
}