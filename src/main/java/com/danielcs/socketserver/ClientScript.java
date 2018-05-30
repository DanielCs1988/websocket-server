package com.danielcs.socketserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientScript {

    public static void main(String[] args) {
        try (
                Socket kkSocket = new Socket("localhost", 5000);
                PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(kkSocket.getInputStream())
                )
        ) {

            String fromServer;
            out.println("route2|{'name': 'Jake', 'age': 19}");
            while (!(fromServer = in.readLine()).equals("EOF")) {
                System.out.println("Server: " + fromServer);
            }

        } catch (IOException e) {
            System.out.println("Connection was terminated.");
        }
    }

}
