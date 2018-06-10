package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.*;
import com.danielcs.socketserver.annotations.AuthGuard;
import com.danielcs.socketserver.annotations.SocketController;
import com.danielcs.socketserver.annotations.SocketHandler;

@SocketController
public class Test {

    @SocketHandler(route = "name")
    public void echoName(SocketContext ctx, String payload) {
        ctx.setProperty("name", payload);
        ctx.reply("name", "Current user: " + ctx.getProperty("name"));
    }

    @SocketHandler(route = "private")
    public void sendToTest(SocketContext ctx, String target) {
        ctx.sendToUser("name", target, "private", "LEKEK");
    }

    @SocketHandler(route = "chat")
    public void sendMessage(SocketContext ctx, String msg) {
        System.out.println(msg);
        ctx.emit("chat", msg);
    }

    @SocketHandler(route = "object", type = Person.class)
    public void sendObject(SocketContext ctx, Person person) {
        ctx.emit("object", person);
    }

    @SocketHandler(route = "ingredients/get")
    public void getIngredients(SocketContext ctx, String msg) {
        ctx.reply(Ingredient.ingredients);
    }
}
