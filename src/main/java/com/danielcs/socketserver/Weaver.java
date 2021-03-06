package com.danielcs.socketserver;

import com.danielcs.socketserver.annotations.Weave;

import java.lang.reflect.Method;
import java.util.Map;

final class Weaver {

    private final Map<String, AspectInvoker> aspects;
    private final Map<Method, MethodInvoker> controllers;

    Weaver(Map<String, AspectInvoker> aspects, Map<Method, MethodInvoker> controllers) {
        this.aspects = aspects;
        this.controllers = controllers;
    }

    Object invoke(Method method, Object... args) {
        if (!method.isAnnotationPresent(Weave.class)) {
            return controllers.get(method).invoke(args);
        }
        String aspectName = method.getAnnotation(Weave.class).aspect();
        AspectInvoker aspect = aspects.get(aspectName);
        MethodInvoker original = controllers.get(method);
        Object[] methodNameAndArgs = new Object[args.length + 1];
        methodNameAndArgs[0] = method.getName();
        System.arraycopy(args, 0, methodNameAndArgs, 1, args.length);

        switch (aspect.getType()) {
            case BEFORE:
                aspect.invoke(methodNameAndArgs);
                return original.invoke(args);
            case AFTER:
                Object retVal = original.invoke(args);
                aspect.invoke(retVal);
                return retVal;
            case INTERCEPTOR:
                boolean canCall = (boolean)aspect.invoke(methodNameAndArgs);
                return canCall ? original.invoke(args) : null;
            case PREPROCESSOR:
                Object[] processed = (Object[]) aspect.invoke(methodNameAndArgs);
                return original.invoke(processed);
            case POSTPROCESSOR:
                Object originalValue = original.invoke(args);
                return aspect.invoke(originalValue);
        }
        return null;
    }
}
