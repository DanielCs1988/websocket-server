package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.AuthGuard;
import com.danielcs.socketserver.SocketContext;

public class SecondAuth implements AuthGuard {

    private LoggerService logger;

    public SecondAuth(LoggerService logger) {
        this.logger = logger;
    }

    @Override
    public boolean authorize(SocketContext ctx, String token) {
        logger.log("Second auth running woooo.... writing the token into the context like there is no tomorrow!");
        ctx.setProperty("token", token);
        return true;
    }
}
