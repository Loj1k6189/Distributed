package com.example.distributed.quest.exception;

import lombok.Getter;

/**
 * 问卷业务异常类
 */
@Getter
public class QuestionnaireException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String message;

    public QuestionnaireException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.message = errorCode.getMessage();
    }

    public QuestionnaireException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.message = detailMessage;
    }

    /**
     * 错误码枚举
     */
    @Getter
    public enum ErrorCode {
        QUESTIONNAIRE_NOT_FOUND(1001, "问卷不存在"),
        QUESTION_NOT_FOUND(1002, "题目不存在"),
        QUESTIONNAIRE_INACTIVE(1003, "问卷已禁用"),
        QUESTIONNAIRE_NOT_STARTED(1004, "问卷未开始"),
        QUESTIONNAIRE_EXPIRED(1005, "问卷已过期"),
        DUPLICATE_SUBMISSION(1006, "重复提交"),
        MAX_SUBMISSIONS_REACHED(1007, "已达到最大提交次数"),
        INVALID_ANSWER(1008, "答案格式不正确"),
        REQUIRED_QUESTION_MISSING(1009, "必填题目未作答"),
        QUESTIONNAIRE_LOCKED(1010, "问卷正在处理中，请稍后重试"),
        OPTION_NOT_FOUND(1011, "选项不存在");

        private final int code;
        private final String message;

        ErrorCode(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
