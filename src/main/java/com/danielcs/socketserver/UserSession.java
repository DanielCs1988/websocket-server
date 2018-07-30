package com.danielcs.socketserver;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public final class UserSession {

    private final static int MESSAGE_QUEUE_CAPACITY = 5;
    private static int numberOfUsers;

    private final int id;
    private final Set<String> rooms = new HashSet<>();
    private final Map<String, Object> properties = new HashMap<>();
    private final ArrayBlockingQueue<String> messages = new ArrayBlockingQueue<>(MESSAGE_QUEUE_CAPACITY);

    UserSession() {
        id = numberOfUsers++;
    }

    public static int getNumberOfUsers() {
        return numberOfUsers;
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

    int getId() {
        return id;
    }

    Set<String> getRooms() {
        return Collections.unmodifiableSet(rooms);
    }

    Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    void clearProperties() {
        properties.clear();
    }

    void setProperty(String name, Object property) {
        properties.put(name, property);
    }

    Object getProperty(String name) {
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

    @Override
    public String toString() {
        return "UserSession{" +
                "id=" + id +
                ", properties=" + properties +
                '}';
    }
}
