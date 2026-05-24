package com.example.distributed.chain.exception;

import lombok.Getter;

/**
 * 接龙业务异常类
 */
@Getter
public class ChainException extends RuntimeException {

    private final ErrorCode errorCode;

    public ChainException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ChainException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 错误码枚举
     */
    public enum ErrorCode {
        CHAIN_NOT_FOUND("CHAIN_001", "接龙不存在"),
        CHAIN_INACTIVE("CHAIN_002", "接龙已停止"),
        CHAIN_FULL("CHAIN_003", "接龙已满"),
        CHAIN_EXPIRED("CHAIN_004", "接龙已过期"),
        CHAIN_NOT_STARTED("CHAIN_005", "接龙尚未开始"),
        DUPLICATE_ENTRY("CHAIN_006", "您已参与此接龙"),
        MULTIPLE_NOT_ALLOWED("CHAIN_007", "此接龙不允许重复参与"),
        MAX_ENTRIES_REACHED("CHAIN_008", "接龙项数已达上限"),
        CHAIN_LOCKED("CHAIN_009", "接龙正在被编辑，请稍后重试"),
        INVALID_SEQUENCE("CHAIN_010", "接龙序号无效");

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
