package com.danielcs.socketserver;

import com.danielcs.socketserver.annotations.*;
import com.danielcs.socketserver.request.RequestHandlerFactory;
import com.google.gson.Gson;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SocketServer implements Server {

    private static final String ON_CONNECT = "connect";
    private static final String ON_DISCONNECT = "disconnect";

    private final int PORT;
    private final String CLASSPATH;
    private final Map<Class, Object> dependencies = new HashMap<>();
    private final ExecutorService connectionPool;
    // TODO: MAY work better with a MAP, too many lookups
    private final Set<UserSession> users = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, Controller> controllers = new HashMap<>();
    private final Gson converter = new Gson();
    private final MessageFormatter formatter = new BasicMessageFormatter(); // TODO: Can make this customizable!

    private HandlerInvoker connectHandler;
    private HandlerInvoker disconnectHandler;
    private Injector injector;

    public SocketServer(int port, String classpath) {
        this(port, classpath, 20);
    }

    public SocketServer(int port, String classPath, int poolSize) {
        this.PORT = port;
        this.CLASSPATH = classPath;
        connectionPool = Executors.newFixedThreadPool(poolSize * 2);
        initAppContainer();
    }

    private void initAppContainer() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(CLASSPATH))
                .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner(), new MethodAnnotationsScanner())
        );

        Set<Class<?>> handlerClasses = reflections.getTypesAnnotatedWith(SocketController.class);
        Set<Class<?>> configClasses = reflections.getTypesAnnotatedWith(Configuration.class);
        Set<Class<? extends AuthGuard>> authGuards = reflections.getSubTypesOf(AuthGuard.class);

        Set<Class<?>> assemblers = reflections.getTypesAnnotatedWith(HttpRequestAssembler.class);
        Object proxy = RequestHandlerFactory.createProxy(assemblers, converter);

        Set<Method> aspects = reflections.getMethodsAnnotatedWith(Aspect.class);
        Set<Class> fabric = reflections.getMethodsAnnotatedWith(Weave.class).stream()
                .map(Method::getDeclaringClass)
                .collect(Collectors.toSet());

        initRequestHandlers(assemblers, proxy);
        initDependencies(configClasses);  // Injector instance is set up at the end of this method call
        Weaver weaver = WeaverFactory.createWeaver(fabric, aspects, injector);
        setupControllers(handlerClasses, fabric, weaver);
        setAuthGuards(authGuards);
    }

    private void initRequestHandlers(Set<Class<?>> assemblers, Object proxy) {
        for (Class<?> assembler : assemblers) {
            dependencies.put(assembler, proxy);
        }
    }

    private void initDependencies(Set<Class<?>> configClasses) {
        for (Class<?> configClass : configClasses) {
            try {
                Object configObject = configClass.newInstance();
                for (Method method : configClass.getMethods()) {
                    if (method.isAnnotationPresent(Dependency.class)) {
                        dependencies.put(method.getReturnType(), method.invoke(configObject));
                    }
                }
                resolveDependencies();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                System.out.println("Could not initialize dependencies.");
                e.printStackTrace();
            }
        }
    }

    private void resolveDependencies() throws InvocationTargetException, IllegalAccessException {
        for (Class dependency : dependencies.keySet()) {
            Object depObject = dependencies.get(dependency);
            for (Method method : depObject.getClass().getMethods()) {
                if (method.isAnnotationPresent(InjectionPoint.class)) {
                    Class classNeeded = method.getParameterTypes()[0];
                    method.invoke(depObject, dependencies.get(classNeeded));
                }
            }
        }
        injector = new Injector(dependencies);
    }

    private void setupControllers(Set<Class<?>> handlerClasses, Set<Class> fabric, Weaver weaver) {
        for (Class handlerClass : handlerClasses) {
            boolean shouldWeave = fabric.contains(handlerClass);
            Object instance = shouldWeave ? weaver : injector.injectDependencies(handlerClass);
            for (Method method : handlerClass.getMethods()) {
                if (method.isAnnotationPresent(OnMessage.class)) {
                    createController(instance, method, shouldWeave);
                }
            }
        }
    }

    private void createController(Object instance, Method method, boolean shouldWeave) {
        OnMessage config = method.getAnnotation(OnMessage.class);
        final String route = config.route();
        final Class type = config.type();

        switch (route) {
            case ON_CONNECT:
                connectHandler = shouldWeave ?
                        new HandlerInvoker(instance, method, true) :
                        new HandlerInvoker(instance, method);
                break;
            case ON_DISCONNECT:
                disconnectHandler = shouldWeave ?
                        new HandlerInvoker(instance, method, true) :
                        new HandlerInvoker(instance, method);
                break;
            default:
                if (shouldWeave) {
                    controllers.put(route, new Controller(instance, method, type, converter, true));
                } else {
                    controllers.put(route, new Controller(instance, method, type, converter));
                }
        }
    }

    private void setAuthGuards(Set<Class<? extends AuthGuard>> authGuards) {
        for (Class<? extends AuthGuard> authGuard : authGuards) {
            AuthGuard guard = (AuthGuard) injector.injectDependencies(authGuard);
            SocketTransactionUtils.registerAuthGuard(guard);
        }
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("WebSocket server started on port " + PORT + ". Listening for connections...");

            while (true) {
                Socket client = server.accept();

                UserSession user = new UserSession();
                users.add(user);
                BasicContext ctx = new BasicContext(user, users);
                MessageSender handler = new MessageSender(client, user);
                MessageBroker broker = new MessageBroker(
                        client, ctx, controllers, connectHandler, disconnectHandler, formatter
                );

                connectionPool.execute(handler);
                connectionPool.execute(broker);
                System.out.println("Client connected.");
            }

        } catch (IOException e) {
            System.out.println("Could not open server-side socket connection.");
        }
        connectionPool.shutdownNow();
        System.out.println("Server closed down.");
    }
}
