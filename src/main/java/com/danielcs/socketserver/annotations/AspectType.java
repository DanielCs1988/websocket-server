package com.danielcs.socketserver.annotations;

/**
 * BEFORE: Runs aspect before the woven method and receives it's arguments.
 * AFTER: Runs aspect after the woven method and receives it's return value.
 * INTERCEPTOR: Runs aspect with the arguments of the woven method, and only executes it if the
 *              aspect returns true.
 * PREPROCESSOR: Runs aspect with the arguments of the woven method, then the aspect must return those arguments,
 *               potentially transforming them in the process. The woven method is then called with those arguments.
 * POSTPROCESSOR: Runs the woven method, but it's return value is given to the aspect, then the aspect's return
 *                value is returned to the calling environment.
 */
public enum AspectType {
    BEFORE,
    AFTER,
    INTERCEPTOR,
    PREPROCESSOR,
    POSTPROCESSOR
}
