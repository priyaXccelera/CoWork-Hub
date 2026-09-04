package com.example.app.exception;

import com.example.app.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList());
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Validation failed",
            request.getRequestURI(),
            details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRule(
      BusinessRuleException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(
      ForbiddenException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(),
            "Access is denied",
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            ex.getStatusCode().value(),
            ex.getStatusCode().toString(),
            ex.getReason(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(ex.getStatusCode()).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }
}
