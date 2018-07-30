package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.annotations.Configuration;
import com.danielcs.socketserver.annotations.Dependency;
import org.jose4j.jwk.HttpsJwks;

@Configuration
public class AppConfig {

    @Dependency
    public LoggerService getLogger() {
        return new LoggerService();
    }

    @Dependency
    public HttpsJwks getJwks() {
        return new HttpsJwks(System.getenv("JWKS_ENDPOINT"));
    }

}
