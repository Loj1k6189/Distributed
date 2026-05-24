package com.example.distributed.chain.controller;

import com.example.distributed.chain.exception.ChainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 接龙全局异常处理器
 */
@RestControllerAdvice(basePackages = "com.example.distributed.chain")
@Slf4j
public class ChainExceptionHandler {

    /**
     * 处理接龙业务异常
     */
    @ExceptionHandler(ChainException.class)
    public ResponseEntity<Map<String, Object>> handleChainException(ChainException ex) {
        log.error("接龙业务异常: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", ex.getErrorCode().getCode());
        response.put("message", ex.getMessage());
        
        HttpStatus status;
        switch (ex.getErrorCode()) {
            case CHAIN_NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case DUPLICATE_ENTRY:
            case MULTIPLE_NOT_ALLOWED:
                status = HttpStatus.CONFLICT;
                break;
            case CHAIN_LOCKED:
                status = HttpStatus.TOO_MANY_REQUESTS;
                break;
            case CHAIN_FULL:
            case CHAIN_EXPIRED:
            case CHAIN_NOT_STARTED:
            case CHAIN_INACTIVE:
            case MAX_ENTRIES_REACHED:
            case INVALID_SEQUENCE:
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.BAD_REQUEST;
        }
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "参数验证失败");
        response.put("errors", errors);
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("接龙系统异常: ", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "系统内部错误");
        response.put("error", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
