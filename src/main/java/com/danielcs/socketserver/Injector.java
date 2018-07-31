package com.danielcs.socketserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

final class Injector {

    private final Map<Class, Object> dependencies;

    Injector(Map<Class, Object> dependencies) {
        this.dependencies = dependencies;
    }

    Object injectDependencies(Class processedClass)  {
        Constructor constructor = processedClass.getConstructors()[0];
        Class[] paramClasses =  constructor.getParameterTypes();
        Object[] params = new Object[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            params[i] = dependencies.getOrDefault(paramClasses[i], null);
        }
        try {
            return params.length > 0 ? constructor.newInstance(params) : constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            System.out.println("Could not inject dependencies to class: " + processedClass.getName());
            return null;
        }
    }

}
