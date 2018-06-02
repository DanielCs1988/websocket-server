package com.danielcs.socketserver;

public abstract class MessageFormatter {

    protected String route;
    protected String payload;

    public MessageFormatter(String route, String payload) {
        this.route = route;
        this.payload = payload;
    }

    public MessageFormatter() {
    }

    public String getRoute() {
        return route;
    }
    public String getRawPayload() {
        return payload;
    }

    public abstract void processMessage(String message) throws IllegalArgumentException;
    public abstract String assembleMessage() throws IllegalStateException;
}
