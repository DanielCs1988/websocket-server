package com.danielcs.socketserver;

import com.danielcs.socketserver.annotations.OnMessage;

import java.lang.reflect.Method;
import java.util.*;

final class WeaverFactory {

    static Weaver createWeaver(Set<Class> fabric, Set<Method> aspects, Injector injector) {
        Map<String, AspectInvoker> aspectsInvokers = createAspects(aspects, injector);
        Map<Method, MethodInvoker> wovenControllers = createWovenControllers(fabric, injector);
        return new Weaver(aspectsInvokers, wovenControllers);
    }

    private static Map<Method, MethodInvoker> createWovenControllers(Set<Class> fabric, Injector injector) {
        Map<Method, MethodInvoker> controllers = new HashMap<>();
        for (Class controller : fabric) {
            Object instance = injector.injectDependencies(controller);
            for (Method method : controller.getMethods()) {
                if (method.isAnnotationPresent(OnMessage.class)) {
                    controllers.put(method, new MethodInvoker(instance, method));
                }
            }
        }
        return controllers;
    }

    private static Map<String, AspectInvoker> createAspects(Set<Method> aspects, Injector injector) {
        Map<String, AspectInvoker> aspectsManifest = new HashMap<>();
        Map<Class, Object> aspectObjects = new HashMap<>();
        for (Method aspect : aspects) {
            Class aspectClass = aspect.getDeclaringClass();
            if (!aspectObjects.containsKey(aspectClass)) {
                aspectObjects.put(aspectClass, injector.injectDependencies(aspectClass));
            }
            // If at a later time aspects can declare their name in the annotation, this is where it can be changed.
            aspectsManifest.put(aspect.getName(), new AspectInvoker(
                    aspectObjects.get(aspectClass), aspect
            ));
        }
        return aspectsManifest;
    }

}
