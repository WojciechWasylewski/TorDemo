package org.example.tordemo.exitnode.exception;

public class ExitNodesUnavailableException extends RuntimeException {

    public ExitNodesUnavailableException() {
        super("Tor exit-node data is unavailable or expired");
    }
}
