package com.danielcs.socketserver.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.danielcs.socketserver.Server;
import com.danielcs.socketserver.SocketServer;
import com.danielcs.socketserver.annotations.AuthGuard;

import java.io.UnsupportedEncodingException;

public class Run {

    /*@AuthGuard
    public static boolean processTokenValidity(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(System.getenv("SECRET_KEY"));
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(System.getenv("ISSUER"))
                    .withAudience(System.getenv("AUDIENCE"))
                    .build();
            DecodedJWT jwt = verifier.verify(token);
            return true;
        } catch (UnsupportedEncodingException e) {
            System.out.println("ERROR occurred while getting encoding algorithm: UTF-8 encoding not supported");
        } catch (Exception e) {
            System.out.println("Received invalid token!");
        }
        return false;
    }*/

    public static void main(String[] args) {
        Server server = new SocketServer(8080, "com.danielcs.socketserver.controllers");
        server.start();
    }

}
