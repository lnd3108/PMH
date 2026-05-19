package com.example.demo.common.exception;


import lombok.Getter;

@Getter
public class BusinessException  extends RuntimeException{

    private final String code;
    private final int status;

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus().value();
    }

    public BusinessException(ErrorCode errorCode, String message){
        super(message);
        this.code = errorCode.getCode();
        this.status = errorCode.getStatus().value();
    }
}
