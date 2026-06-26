package com.worldsbank.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
//global watcher sits above all controllers
public class GlobalExceptionHandler {

    // ─── RUNTIME EXCEPTIONS ──────────────────────────────────
    // Catches all our custom RuntimeExceptions from services
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiException> handleRuntimeException(
            RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiException.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ─── WRONG PASSWORD / EMAIL ──────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiException> handleBadCredentials(
            BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiException.builder()
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .message("Invalid email or password")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ─── ACCOUNT NOT VERIFIED ────────────────────────────────
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiException> handleDisabledException(
            DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiException.builder()
                        .status(HttpStatus.FORBIDDEN.value())
                        .message("Account not verified. Check your email for OTP.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ─── VALIDATION ERRORS ───────────────────────────────────
    // Catches @NotBlank, @Email, @Positive etc. failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult()
                .getAllErrors()
                .forEach(error -> {
                    String fieldName = ((FieldError) error).getField();
                    String errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errors);
    }

    // ─── GENERIC FALLBACK ────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiException> handleGenericException(
            Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiException.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("Something went wrong. Please try again.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}