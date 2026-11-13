package org.example.tordemo.exitnode.exception;

public class InvalidTorResponseException extends RuntimeException {

    public InvalidTorResponseException(String message) {
        super(message);
    }
}
