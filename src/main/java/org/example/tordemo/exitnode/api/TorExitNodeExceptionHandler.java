package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TorExitNodeExceptionHandler {

    @ExceptionHandler(ExitNodesUnavailableException.class)
    public ResponseEntity<Void> handleUnavailableData() {
        return ResponseEntity.status(503).build();
    }
}
