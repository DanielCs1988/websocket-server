package com.danielcs.socketserver;

import com.google.gson.Gson;

import java.util.Set;

class BasicContext implements SocketContext {

    private boolean connected = true;
    private final Gson converter = new Gson();
    private final UserSession user;
    private final Set<UserSession> users;

    BasicContext(UserSession user, Set<UserSession> users) {
        this.user = user;
        this.users = users;
    }

    private String getFormattedMessage(String path, Object payload) {
        String payloadJson = converter.toJson(payload);
        MessageFormatter f = new BasicMessageFormatter(path, payloadJson);
        return f.assembleMessage();
    }

    @Override
    public boolean connected() {
        return connected;
    }

    @Override
    public UserSession getUser() {
        return user;
    }

    @Override
    public void reply(String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        user.sendMessage(msg);
    }

    @Override
    public void emit(String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        users.forEach(user -> user.sendMessage(msg));
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
    public void leaveAllRooms() {
        Room.leaveCurrentRooms(user);
    }

    @Override
    public void emitToRoom(String room, String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        Room.getUsersInRoom(room).forEach(user -> user.sendMessage(msg));
    }

    @Override
    public void sendToUser(int userId, String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        users.stream()
                .filter(usr -> usr.getId() == userId)
                .findFirst()
                .ifPresent(usr -> usr.sendMessage(msg));
    }

    @Override
    public void sendToUser(String propertyName, String propertyValue, String path, Object payload) {
        String msg = getFormattedMessage(path, payload);
        users.stream()
                .filter(usr -> usr.getProperty(propertyName).equals(propertyValue))
                .forEach(usr -> usr.sendMessage(msg));
    }

    @Override
    public void disconnect() {
        connected = false;
    }
}
