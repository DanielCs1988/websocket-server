package com.danielcs.socketserver;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public final class UserSession {

    private static int numberOfUsers;

    private final int id;
    private final Set<String> rooms = new HashSet<>();
    private final Map<String, Object> properties = new HashMap<>();
    private final ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<>(2);

    UserSession() {
        id = numberOfUsers++;
    }

    void sendMessage(String msg) {
        messages.offer(msg);
    }

    ArrayBlockingQueue<String> getMessages() {
        return messages;
    }

    void joinRoom(String name) {
        rooms.add(name);
    }

    void leaveRoom(String name) {
        rooms.remove(name);
    }

    boolean isInRoom(String name) {
        return rooms.contains(name);
    }

    public static int getNumberOfUsers() {
        return numberOfUsers;
    }

    public int getId() {
        return id;
    }

    public Set<String> getRooms() {
        return Collections.unmodifiableSet(rooms);
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public void clearProperties() {
        properties.clear();
    }

    public void setProperty(String name, Object property) {
        properties.put(name, property);
    }

    public Object getProperty(String name) {
        // TODO: may need to error handle here
        return properties.get(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSession)) return false;
        UserSession that = (UserSession) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
