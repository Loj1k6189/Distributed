package com.example.distributed.quest.service;

public class QuestionBusinessException extends RuntimeException {
    private String errorCode;

    public QuestionBusinessException(String message) {
        super(message);
    }

    public QuestionBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public QuestionBusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public QuestionBusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}