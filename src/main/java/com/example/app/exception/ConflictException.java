package com.example.app.exception;

/**
 * Raised when a request conflicts with the current state of a resource (double booking, space at
 * capacity, deleting a membership plan still in use, cancelling an already-cancelled booking,
 * duplicate email, etc). Always mapped to HTTP 409 Conflict by the {@link GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
