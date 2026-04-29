package com.example.distributed.lottery.service;

import org.springframework.http.HttpStatus;

public class LotteryBusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public LotteryBusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
