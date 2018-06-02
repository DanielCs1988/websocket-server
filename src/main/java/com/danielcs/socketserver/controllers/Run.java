package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.SocketServer;

public class Run {

    public static void main(String[] args) {
        SocketServer server = new SocketServer(5500, "com.danielcs.socketserver.controllers", 10);
        server.start();
    }

}
