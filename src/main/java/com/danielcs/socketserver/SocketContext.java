package com.danielcs.socketserver;

import java.util.Map;
import java.util.Set;

public interface SocketContext {

    void reply(Object payload);
    void reply(String path, Object payload);
    void emit(String path, Object payload);
    void emitToRoom(String room, String path, Object payload);
    void sendToUser(String propertyName, Object propertyValue, String path, Object payload);

    void joinRoom(String name);
    void leaveRoom(String name);
    Set<String> getCurrentRooms();
    void leaveAllRooms();

    Object getProperty(String name);
    Map<String, Object> getProperties();
    void setProperty(String name, Object property);
    void clearProperties();

    void disconnect();
}
