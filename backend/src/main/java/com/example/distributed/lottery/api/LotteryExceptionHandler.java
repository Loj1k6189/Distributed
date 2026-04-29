package com.example.distributed.lottery.api;

import com.example.distributed.lottery.service.LotteryBusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LotteryExceptionHandler {

    @ExceptionHandler(LotteryBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(LotteryBusinessException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null ? firstError.getDefaultMessage() : "请求参数不合法";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_ARGUMENT", message));
    }
}
