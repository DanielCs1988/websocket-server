package com.danielcs.socketserver;

public interface SocketContext {
    void reply(String path, Object payload);
    void emit(String path, Object payload);
    void joinRoom(String name);
    void leaveRoom(String name);
    void leaveAllRooms();
}
