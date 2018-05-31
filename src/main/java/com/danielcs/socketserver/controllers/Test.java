package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.*;

@SocketController
public class Test {

    @SocketHandler(route = "route1")
    public void test1(SocketContext ctx, String payload) {
        System.out.println("Received payload: " + payload);
        ctx.joinRoom("KEK");
        ctx.reply("route1", "Current user: " + Thread.currentThread().getName());
    }

    @SocketHandler(route = "route2", type = Person.class)
    public void test2(SocketContext ctx, Person person) {
        System.out.println("Received person: " + person);
        ctx.emit("route2", person);
    }

}
