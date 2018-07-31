package com.danielcs.socketserver.request;

import com.google.gson.Gson;

import java.lang.reflect.Proxy;
import java.util.Set;

public class RequestHandlerFactory {

    public static Object createProxy(Set<Class<?>> assemblers, Gson converter) {
        return Proxy.newProxyInstance(
                RequestHandlerFactory.class.getClassLoader(),
                assemblers.toArray(new Class[0]),
                new RequestInvoker(new HttpRequestBuilder(), converter)
        );
    }
}
