package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.SocketController;
import com.danielcs.socketserver.SocketHandler;

@SocketController
public class Test {

    @SocketHandler(route = "route1")
    public void test1(String payload) {
        System.out.println("Received payload: " + payload);
    }

    @SocketHandler(route = "route2", type = Person.class)
    public void test2(Person person) {
        System.out.println("Received person: " + person);
    }

}
