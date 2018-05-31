package com.danielcs.socketserver;

import java.util.*;

public abstract class UserSession {

    private static int numberOfUsers;

    private final int id;
    private final Set<String> rooms = new HashSet<>();

    UserSession() {
        id = numberOfUsers++;
    }

    void joinRoom(String name) {
        rooms.add(name);
    }

    void leaveRoom(String name) {
        rooms.remove(name);
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
