package com.danielcs.socketserver;

public interface MessageFormat {
    void processMessage(String message) throws IllegalArgumentException;
    String getRoute();
    String getRawPayload();
}
