package com.danielcs.socketserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class HandlerInvoker {

    protected final Object obj;
    protected final Method method;
    protected final boolean isWoven;

    HandlerInvoker(Object obj, Method method) {
        this.obj = obj;
        this.method = method;
        this.isWoven = false;
    }

    HandlerInvoker(Object obj, Method method, boolean isWoven) {
        this.obj = obj;
        this.method = method;
        this.isWoven = isWoven;
    }

    void call(SocketContext context) {
        try {
            if (isWoven) {
                ((Weaver)obj).invoke(method, context);
            } else {
                method.invoke(obj, context);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("ERROR: could not call lifecycle method " + method.getName());
        }
    }
}
