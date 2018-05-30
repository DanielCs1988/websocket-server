package com.danielcs.socketserver;

public interface Emitter {
    void reply(String path, Object payload);
    void emit(String path, Object payload);
}
