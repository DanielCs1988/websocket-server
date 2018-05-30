package com.danielcs.socketserver;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SocketHandler {

    String route();
    Class type() default String.class;

}
