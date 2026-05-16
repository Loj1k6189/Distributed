package com.example.distributed.vote.service;

import org.springframework.http.HttpStatus;

public class VoteBusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public VoteBusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}

