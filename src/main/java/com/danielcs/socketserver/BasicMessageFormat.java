package com.danielcs.socketserver;

public class BasicMessageFormat extends MessageFormat {

    private static final String SEPARATOR = "&";

    @Override
    public void processMessage(String message) throws IllegalArgumentException {
        String[] fullMsg = message.split(SEPARATOR, 2);
        if (fullMsg.length != 2) {
            throw new IllegalArgumentException("Incoming message format was wrong.");
        }
        route = fullMsg[0];
        payload = fullMsg[1];
    }
}
