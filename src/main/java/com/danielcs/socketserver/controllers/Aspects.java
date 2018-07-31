package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.SocketContext;
import com.danielcs.socketserver.annotations.Aspect;
import com.danielcs.socketserver.annotations.AspectType;

public class Aspects {

    @Aspect(type = AspectType.BEFORE)
    public void logger(Object... args) {
        for (Object arg : args) {
            System.out.println("Received args: " + arg);
        }
    }

    @Aspect(type = AspectType.AFTER)
    public void postLogger(Object retVal) {
        System.out.println("Woven method returned value: " + retVal);
    }

    @Aspect(type = AspectType.INTERCEPTOR)
    public boolean authenticate(Object... args) {
        SocketContext ctx = (SocketContext)args[0];
        int userId = (int)ctx.getProperty("userId");
        System.out.println("Current user's id: " + userId);
        if (userId != 17) {
            ctx.disconnect();
            return false;
        }
        return true;
    }

    @Aspect(type = AspectType.PREPROCESSOR)
    public Object[] process(Object... args) {
        String messageToProcess = args[1].toString();
        messageToProcess += "KEK";
        return new Object[]{args[0], messageToProcess};
    }

    @Aspect(type = AspectType.POSTPROCESSOR)
    public Object postProcess(Object theRetVal) {
        return theRetVal.toString() + " KEK POSTPROCESS";
    }
}
