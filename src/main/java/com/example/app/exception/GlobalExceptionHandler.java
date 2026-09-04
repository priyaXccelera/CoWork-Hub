package com.example.app.exception;

import com.example.app.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // ---------------------------------------------------------------------
  // 400 Bad Request - malformed / invalid input
  // ---------------------------------------------------------------------

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList());
    return badRequest("Validation failed", details, request);
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList());
    return badRequest("Validation failed", details, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    List<String> details =
        ex.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.toList());
    return badRequest("Validation failed", details, request);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String message =
        "Invalid value '"
            + ex.getValue()
            + "' for parameter '"
            + ex.getName()
            + "': expected "
            + (ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "a different type");
    return badRequest(message, null, request);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    return badRequest(
        "Required parameter '" + ex.getParameterName() + "' is missing", null, request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    return badRequest(
        "Malformed request body: the JSON payload could not be parsed", null, request);
  }

  @ExceptionHandler(DateTimeParseException.class)
  public ResponseEntity<ErrorResponse> handleDateTimeParse(
      DateTimeParseException ex, HttpServletRequest request) {
    return badRequest("Invalid date/time value: " + ex.getParsedString(), null, request);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ErrorResponse> handleBusinessRule(
      BusinessRuleException ex, HttpServletRequest request) {
    return badRequest(ex.getMessage(), null, request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    return badRequest(ex.getMessage(), null, request);
  }

  // ---------------------------------------------------------------------
  // 401 Unauthorized
  // ---------------------------------------------------------------------

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            "Missing, invalid or revoked API key",
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  // ---------------------------------------------------------------------
  // 403 Forbidden
  // ---------------------------------------------------------------------

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

  // ---------------------------------------------------------------------
  // 404 Not Found
  // ---------------------------------------------------------------------

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

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            "No route found for " + request.getMethod() + " " + request.getRequestURI(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  // ---------------------------------------------------------------------
  // 405 Method Not Allowed
  // ---------------------------------------------------------------------

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
  }

  // ---------------------------------------------------------------------
  // 409 Conflict
  // ---------------------------------------------------------------------

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(
      ConflictException ex, HttpServletRequest request) {
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

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    log.warn("Data integrity violation on {} {}", request.getMethod(), request.getRequestURI(), ex);
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(),
            "The request could not be completed because it conflicts with existing data",
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  // ---------------------------------------------------------------------
  // Passthrough for explicit ResponseStatusException usages
  // ---------------------------------------------------------------------

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest request) {
    HttpStatus resolved = HttpStatus.resolve(ex.getStatusCode().value());
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            ex.getStatusCode().value(),
            resolved != null ? resolved.getReasonPhrase() : ex.getStatusCode().toString(),
            ex.getReason(),
            request.getRequestURI(),
            null);
    return ResponseEntity.status(ex.getStatusCode()).body(error);
  }

  // ---------------------------------------------------------------------
  // 500 - anything else. Never leak internal details (SQL, class names, stack traces).
  // ---------------------------------------------------------------------

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "An unexpected error occurred. Please try again later.",
            request.getRequestURI(),
            null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  private ResponseEntity<ErrorResponse> badRequest(
      String message, List<String> details, HttpServletRequest request) {
    ErrorResponse error =
        new ErrorResponse(
            LocalDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            message,
            request.getRequestURI(),
            details);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }
}
