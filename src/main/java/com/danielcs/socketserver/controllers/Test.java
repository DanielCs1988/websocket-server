package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.*;
import com.danielcs.socketserver.annotations.SocketController;
import com.danielcs.socketserver.annotations.OnMessage;

@SocketController
public class Test {

    @OnMessage(route = "connect")
    public void connected(SocketContext ctx) {
        System.out.println("User connected!");
    }

    @OnMessage(route = "disconnect")
    public void disconnected(SocketContext ctx) {
        System.out.println("User disconnected!");
    }

    @OnMessage(route = "name")
    public void echoName(SocketContext ctx, String payload) {
        ctx.setProperty("name", payload);
        ctx.reply("name", "Current user: " + ctx.getProperty("name"));
    }

    @OnMessage(route = "private")
    public void sendToTest(SocketContext ctx, String target) {
        ctx.sendToUser("name", target, "private", "LEKEK");
    }

    @OnMessage(route = "chat")
    public void sendMessage(SocketContext ctx, String msg) {
        System.out.println(msg);
        ctx.emit("chat", msg);
    }

    @OnMessage(route = "object", type = Person.class)
    public void sendObject(SocketContext ctx, Person person) {
        ctx.emit("object", person);
    }

    @OnMessage(route = "ingredients/get")
    public void getIngredients(SocketContext ctx, String msg) {
        ctx.reply(Ingredient.ingredients);
    }
}
