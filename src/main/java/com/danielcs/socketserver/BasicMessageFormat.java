package com.danielcs.socketserver;

public class BasicMessageFormat implements MessageFormat {

    private static final String SEPARATOR = "&";
    private String route;
    private String payload;

    @Override
    public void processMessage(String message) throws IllegalArgumentException {
        String[] fullMsg = message.split(SEPARATOR, 2);
        if (fullMsg.length != 2) {
            throw new IllegalArgumentException("Incoming message format was wrong.");
        }
        route = fullMsg[0];
        payload = fullMsg[1];
    }

    @Override
    public String getRoute() {
        return route;
    }

    @Override
    public String getRawPayload() {
        return payload;
    }
}
