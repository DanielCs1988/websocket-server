package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.*;
import com.danielcs.socketserver.annotations.SocketController;
import com.danielcs.socketserver.annotations.OnMessage;
import com.danielcs.socketserver.annotations.Weave;

import java.util.HashMap;
import java.util.Map;

@SocketController
public class Test {

    private final HttpRequest http;

    public Test(HttpRequest http) {
        this.http = http;
    }

    @OnMessage(route = "connect")
    @Weave(aspect = "authenticate")
    public void connected(SocketContext ctx) {
        System.out.println("User connected! Id: " + ctx.getProperty("userId"));
        System.out.println("Token from second authguard: " + ctx.getProperty("token"));
    }

    @OnMessage(route = "disconnect")
    public void disconnected(SocketContext ctx) {
        System.out.println("User disconnected!");
    }

    @OnMessage(route = "name")
    @Weave(aspect = "logger")
    public void echoName(SocketContext ctx, String payload) {
        ctx.setProperty("name", payload);
        ctx.reply("name", "Current user: " + ctx.getProperty("name"));
    }

    @OnMessage(route = "private")
    public void sendToTest(SocketContext ctx, String target) {
        ctx.sendToUser("name", target, "private", "LEKEK");
    }

    @OnMessage(route = "chat")
//    @Weave(aspect = "postLogger")
//    @Weave(aspect = "process")
    @Weave(aspect = "postProcess")
    public String sendMessage(SocketContext ctx, String msg) {
        ctx.emit("chat", msg);
        return msg;
    }

    @OnMessage(route = "object", type = Person.class)
    public void sendObject(SocketContext ctx, Person person) {
        ctx.emit("object", person);
    }

    @OnMessage(route = "ingredients/get")
    public void getIngredients(SocketContext ctx, String msg) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("key", "AIzaSyCRGlfwryYR3LO1s6KwVXEioipAC3A63RE");
        queryParams.put("latlng", "40,20");
        String resp = http.getWithQuery("https://maps.googleapis.com/maps/api/geocode/json", queryParams);
        System.out.println(resp);
        ctx.reply(Ingredient.ingredients);
    }
}
