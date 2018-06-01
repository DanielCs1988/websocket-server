package com.danielcs.socketserver;

public interface SocketContext {
    UserSession getUser();
    void reply(String path, Object payload);
    void emit(String path, Object payload);
    void emitToRoom(String room, String path, Object payload);
    void sendToUser(int userId, String path, Object payload);
    void sendToUser(String propertyName, String propertyValue, String path, Object payload);
    void joinRoom(String name);
    void leaveRoom(String name);
    void leaveAllRooms();
}
