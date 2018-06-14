package com.danielcs.socketserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Caller {

    protected final Object obj;
    protected final Method method;

    Caller(Object obj, Method method) {
        this.obj = obj;
        this.method = method;
    }

    void call(SocketContext ctx) {
        try {
            method.invoke(obj, ctx);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("ERROR: could not call lifecycle method " + method.getName());
        }
    }
}
