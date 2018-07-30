package com.danielcs.socketserver;

import com.google.gson.Gson;

import java.util.Map;
import java.util.Set;

class BasicContext implements SocketContext {

    private boolean connected = true;
    private final Gson converter = new Gson();
    private final UserSession user;
    private final Set<UserSession> users;
    private String currentRoute;

    BasicContext(UserSession user, Set<UserSession> users) {
        this.user = user;
        this.users = users;
    }

    private String getFormattedMessage(String path, Object payload) {
        String payloadJson = converter.toJson(payload);
        MessageFormatter f = new BasicMessageFormatter(path, payloadJson);
        return f.assembleMessage();
    }

    void setCurrentRoute(String currentRoute) {
        this.currentRoute = currentRoute;
    }

    UserSession getUser() {
        return user;
    }

    boolean connected() {
        return connected;
    }

    @Override
    public void reply(String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        user.sendMessage(msg);
    }

    @Override
    public void reply(Object payload) {
        reply(currentRoute, payload);
    }

    @Override
    public void emit(String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        users.forEach(user -> user.sendMessage(msg));
    }

    @Override
    public void emitToRoom(String room, String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        Room.getUsersInRoom(room).forEach(user -> user.sendMessage(msg));
    }

    @Override
    public void sendToUser(String propertyName, Object propertyValue, String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        for (UserSession user : users) {
            Object prop = user.getProperty(propertyName);
            if (prop != null && prop.equals(propertyValue)) {
                user.sendMessage(msg);
            }
        }
    }

    @Override
    public Object getProperty(String name) {
        return user.getProperty(name);
    }

    @Override
    public Map<String, Object> getProperties() {
        return user.getProperties();
    }

    @Override
    public void setProperty(String name, Object property) {
        user.setProperty(name, property);
    }

    @Override
    public void clearProperties() {
        user.clearProperties();
    }

    @Override
    public void joinRoom(String name) {
        Room.joinRoom(user, name);
    }

    @Override
    public void leaveRoom(String name) {
        Room.leaveRoom(user, name);
    }

    @Override
    public Set<String> getCurrentRooms() {
        return user.getRooms();
    }

    @Override
    public void leaveAllRooms() {
        Room.leaveCurrentRooms(user);
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    void removeUser() {
        users.remove(user);
    }
}
