package com.danielcs.socketserver.request;

final class InvalidMethodSignature extends Exception {
    InvalidMethodSignature(String message) {
        super(message);
    }
}
