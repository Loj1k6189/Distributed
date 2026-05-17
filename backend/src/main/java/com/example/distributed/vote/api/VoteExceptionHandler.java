package com.example.distributed.vote.api;

import com.example.distributed.vote.service.VoteBusinessException;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.distributed.vote")
public class VoteExceptionHandler {

    @ExceptionHandler(VoteBusinessException.class)
    public ResponseEntity<VoteErrorResponse> handleVoteException(VoteBusinessException ex) {
        return ResponseEntity.status(ex.httpStatus())
                .body(new VoteErrorResponse(ex.code(), ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<VoteErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("请求参数不合法");
        return ResponseEntity.badRequest()
                .body(new VoteErrorResponse("VALIDATION_ERROR", message, Instant.now()));
    }
}
