package com.prime.prime_app.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input parameters")
                .details(errors)
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "You don't have permission to access this resource",
                request.getDescription(false),
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException ex, WebRequest request) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex, WebRequest request) {
        log.error("JWT token error: {}", ex.getMessage(), ex);
        
        String message = "Authentication token error";
        if (ex instanceof ExpiredJwtException) {
            message = "Your session has expired. Please login again.";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                message,
                request.getDescription(false),
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({AuthenticationException.class, InsufficientAuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, WebRequest request) {
        log.error("Authentication error: {}", ex.getMessage(), ex);
        
        String message = "Authentication failed";
        if (ex instanceof BadCredentialsException) {
            message = "Invalid credentials provided";
        } else if (ex.getMessage().contains("token")) {
            message = "Invalid or expired authentication token";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                message,
                request.getDescription(false),
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred. Please try again later or contact support.",
                request.getDescription(false),
                LocalDateTime.now()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }

    // Helper inner class for structured error responses
    private static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String path;
        private LocalDateTime timestamp;
        private Map<String, String> details;
        
        // Default constructor needed for builder
        public ErrorResponse() {
        }
        
        // Constructor for new implementations
        public ErrorResponse(int status, String message, String path, LocalDateTime timestamp) {
            this.status = status;
            this.message = message;
            this.path = path;
            this.timestamp = timestamp;
        }
        
        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }
        
        public int getStatus() {
            return status;
        }
        
        public String getError() {
            return error;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getPath() {
            return path;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public Map<String, String> getDetails() {
            return details;
        }
        
        // Static builder class
        public static class ErrorResponseBuilder {
            private final ErrorResponse response = new ErrorResponse();
            
            public ErrorResponseBuilder status(int status) {
                response.status = status;
                return this;
            }
            
            public ErrorResponseBuilder error(String error) {
                response.error = error;
                return this;
            }
            
            public ErrorResponseBuilder message(String message) {
                response.message = message;
                return this;
            }
            
            public ErrorResponseBuilder path(String path) {
                response.path = path;
                return this;
            }
            
            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                response.timestamp = timestamp;
                return this;
            }
            
            public ErrorResponseBuilder details(Map<String, String> details) {
                response.details = details;
                return this;
            }
            
            public ErrorResponse build() {
                return response;
            }
        }
    }
}