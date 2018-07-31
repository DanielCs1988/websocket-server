package com.danielcs.socketserver;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class Controller extends HandlerInvoker {

    private final Class type;
    private final Gson converter;

    Controller(Object obj, Method method, Class type, Gson converter) {
        super(obj, method);
        this.type = type;
        this.converter = converter;
    }

    Controller(Object obj, Method method, Class type, Gson converter, boolean isWoven) {
        super(obj, method, isWoven);
        this.type = type;
        this.converter = converter;
    }

    void handle(SocketContext context, String rawInput) {
        try {
            Object payload = type == String.class ? rawInput : converter.fromJson(rawInput, type);
            if (isWoven) {
                ((Weaver) obj).invoke(method, context, payload);
            } else {
                method.invoke(obj, context, payload);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("Controller call failed: " + method.getName());
        } catch (JsonSyntaxException ee) {
            System.out.println("JSON format was invalid.");
        }
    }
}
