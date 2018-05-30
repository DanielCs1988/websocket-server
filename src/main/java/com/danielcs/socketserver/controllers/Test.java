package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.Emitter;
import com.danielcs.socketserver.SocketController;
import com.danielcs.socketserver.SocketHandler;

@SocketController
public class Test {

    @SocketHandler(route = "route1")
    public void test1(Emitter out, String payload) {
        System.out.println("Received payload: " + payload);
        out.reply("route1", payload);
    }

    @SocketHandler(route = "route2", type = Person.class)
    public void test2(Emitter out, Person person) {
        System.out.println("Received person: " + person);
        out.emit("route2", person);
    }

}
