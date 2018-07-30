package com.danielcs.socketserver;

public interface AuthGuard {
    boolean authorize(SocketContext ctx, String token);
}
