package com.danielcs.socketserver;

import java.util.*;

class Room {

    private static final Set<Room> rooms = new HashSet<>();

    private final String name;
    private final Set<UserSession> users = new HashSet<>();

    private Room(String name) {
        this.name = name;
    }

    private boolean userInRoom(UserSession user) {
        return users.contains(user);
    }

    static Set<UserSession> getUsersInRoom(String name) {
        return rooms.stream()
                .filter(r -> r.name.equals(name))
                .findFirst()
                .map(r -> r.users)
                .orElse(null);
    }

    private static Room findOrCreateRoom(String name) {
        Optional<Room> room = rooms.stream().filter(r -> r.name.equals(name)).findFirst();
        return room.orElseGet(() -> createRoom(name));
    }

    private static Room createRoom(String name) {
        Room room = new Room(name);
        rooms.add(room);
        return room;
    }

    static void joinRoom(UserSession user, String name) {
        findOrCreateRoom(name).users.add(user);
        user.joinRoom(name);
    }

    static void leaveRoom(UserSession user, String name) {
        findOrCreateRoom(name).users.remove(user);
        user.leaveRoom(name);
    }

    static void leaveCurrentRooms(UserSession user) {
        rooms.stream()
                .filter(room -> room.userInRoom(user))
                .forEach(room -> leaveRoom(user, room.name));
    }

    @Override
    public String toString() {
        return "Room{" +
                "name='" + name + '\'' +
                ", users=" + users +
                '}';
    }
}
