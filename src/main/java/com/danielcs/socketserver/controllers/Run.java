package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.Server;
import com.danielcs.socketserver.SocketServer;

public class Run {

    public static void main(String[] args) {
        Server server = new SocketServer(8080, "com.danielcs.socketserver.controllers");
        server.start();
    }

}
