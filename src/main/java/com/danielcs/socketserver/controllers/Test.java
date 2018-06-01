package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.*;

@SocketController
public class Test {

    @SocketHandler(route = "name")
    public void test1(SocketContext ctx, String payload) {
        ctx.getUser().setProperty("name", payload);
        ctx.reply("name", "Current user: " + ctx.getUser().getId() + ", " + ctx.getUser().getProperty("name"));
    }

    @SocketHandler(route = "chat")
    public void sendMessage(SocketContext ctx, String target) {
        ctx.sendToUser("name", target, "chat", "LEKEK BRO");
    }

    @SocketHandler(route = "object", type = Person.class)
    public void test2(SocketContext ctx, Person person) {
        ctx.emit("object", person);
    }

}
