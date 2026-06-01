package com.example.distributed.quest.controller;

import com.example.distributed.quest.exception.QuestionnaireException;
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
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class QuestionnaireExceptionHandler {

    /**
     * 处理问卷业务异常
     */
    @ExceptionHandler(QuestionnaireException.class)
    public ResponseEntity<Map<String, Object>> handleQuestionnaireException(QuestionnaireException ex) {
        log.error("问卷业务异常: code={}, message={}", ex.getErrorCode().getCode(), ex.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", ex.getErrorCode().getCode());
        response.put("message", ex.getMessage());
        
        HttpStatus status;
        switch (ex.getErrorCode()) {
            case QUESTIONNAIRE_NOT_FOUND:
            case QUESTION_NOT_FOUND:
            case OPTION_NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case DUPLICATE_SUBMISSION:
            case MAX_SUBMISSIONS_REACHED:
                status = HttpStatus.CONFLICT;
                break;
            case QUESTIONNAIRE_LOCKED:
                status = HttpStatus.TOO_MANY_REQUESTS;
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
        log.error("系统异常: ", ex);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "系统内部错误");
        response.put("error", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
