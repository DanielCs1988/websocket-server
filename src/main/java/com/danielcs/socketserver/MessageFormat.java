package com.danielcs.socketserver;

public abstract class MessageFormat {

    protected String route;
    protected String payload;

    public String getRoute() {
        return route;
    }
    public String getRawPayload() {
        return payload;
    }

    public abstract void processMessage(String message) throws IllegalArgumentException;
}
