package org.example.onlinemart.aop;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Aspect
@RestControllerAdvice
public class GlobalExceptionHandlerAspect {

    @AfterThrowing(pointcut = "within(@org.springframework.web.bind.annotation.RestController *))", throwing = "ex")
    public ResponseEntity<?> handleControllerException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}