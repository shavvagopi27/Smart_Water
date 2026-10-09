package com.example.smartwater.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** One place that turns every exception into the consistent ApiError JSON shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) { return build(HttpStatus.NOT_FOUND, ex.getMessage()); }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> noResource(NoResourceFoundException ex) { return build(HttpStatus.NOT_FOUND, "Resource not found"); }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex) { return build(HttpStatus.BAD_REQUEST, ex.getMessage()); }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflict(ConflictException ex) { return build(HttpStatus.CONFLICT, ex.getMessage()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "The operation conflicts with existing data (duplicate value or the record is still referenced by other data)");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.of(400, "Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraint(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v -> errors.put(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.badRequest().body(ApiError.of(400, "Validation failed", errors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> unreadable(Exception ex) { return build(HttpStatus.BAD_REQUEST, "Malformed request: check parameter types, required fields and date formats (yyyy-MM-dd)"); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException ex) { return build(HttpStatus.BAD_REQUEST, "Uploaded file is too large (max 5MB)"); }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException ex) { return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported"); }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> badCredentials(BadCredentialsException ex) { return build(HttpStatus.UNAUTHORIZED, "Invalid email or password"); }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> auth(AuthenticationException ex) { return build(HttpStatus.UNAUTHORIZED, "Authentication required"); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException ex) { return build(HttpStatus.FORBIDDEN, "You do not have permission to perform this action"); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> other(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");
    }
}
