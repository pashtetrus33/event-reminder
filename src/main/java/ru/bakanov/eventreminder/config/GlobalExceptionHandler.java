package ru.bakanov.eventreminder.config;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.bakanov.eventreminder.shared.exception.DomainException;
import ru.bakanov.eventreminder.shared.exception.ResourceNotFoundException;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Environment environment;

    GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = ex.getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Validation Error");
        pd.setProperty("errors", errors);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handle(DomainException e) {
        LOG.warn("Domain exception: {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Bad Request");
        pd.setProperty("errors", List.of(e.getMessage()));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handle(ResourceNotFoundException e) {
        LOG.warn("Not found: {}", e.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setTitle("Resource Not Found");
        pd.setProperty("errors", List.of(e.getMessage()));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        LOG.error("Unexpected exception", e);
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        String detail = isDev ? e.getMessage() : "An unexpected error occurred";
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detail);
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
