package com.danielcs.socketserver;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MethodInvoker {

    protected final Object obj;
    protected final Method method;

    public MethodInvoker(Object obj, Method method) {
        this.obj = obj;
        this.method = method;
    }

    Object invoke(Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("Error when invoking method " + method.getName() + "!");
        }
        return null;
    }
}
