package com.example.app.exception;

/**
 * Raised when a request violates a business validation rule (e.g. endTime before startTime, a
 * required field is missing in context, etc). Always mapped to HTTP 400 Bad Request by the {@link
 * GlobalExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }
}
