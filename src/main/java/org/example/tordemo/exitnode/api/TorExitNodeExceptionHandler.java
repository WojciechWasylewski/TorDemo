package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.api.response.InvalidIpv4Response;
import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;
import org.example.tordemo.exitnode.validation.IPv4;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class TorExitNodeExceptionHandler {

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<InvalidIpv4Response> handleInvalidIpv4(
            HandlerMethodValidationException exception) {
        boolean invalidIpv4 = exception.getParameterValidationResults().stream()
                .anyMatch(result -> result.getMethodParameter().hasParameterAnnotation(IPv4.class));
        if (exception.isForReturnValue() || !invalidIpv4) {
            throw exception;
        }

        InvalidIpv4Response response = new InvalidIpv4Response(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "The IP address must be a valid IPv4 address."
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ExitNodesUnavailableException.class)
    public ResponseEntity<Void> handleUnavailableData() {
        return ResponseEntity.status(503).build();
    }
}
