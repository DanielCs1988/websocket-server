package com.danielcs.socketserver;

import com.danielcs.socketserver.annotations.*;
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

public class SocketServer implements Server {

    private final int PORT;
    private final String CLASSPATH;
    private final Map<Class, Object> dependencies = new HashMap<>();
    private final ExecutorService connectionPool;
    // TODO: MAY work better with a MAP, too many lookups
    private final Set<UserSession> users = Collections.synchronizedSet(new HashSet<>());
    private final Map<Class, Map<String, Controller>> controllers = new HashMap<>();

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

        initDependencies(configClasses);
        setupControllers(handlerClasses);
        setAuthGuards(authGuards);
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
    }

    Object injectDependencies(Class processedClass) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor constructor = processedClass.getConstructors()[0];
        Class[] paramClasses =  constructor.getParameterTypes();
        Object[] params = new Object[paramClasses.length];
        for (int i = 0; i < paramClasses.length; i++) {
            params[i] = dependencies.getOrDefault(paramClasses[i], null);
        }
        return params.length > 0 ? constructor.newInstance(params) : constructor.newInstance();
    }

    private void setupControllers(Set<Class<?>> handlerClasses) {
        for (Class handlerClass : handlerClasses) {
            controllers.put(handlerClass, new HashMap<>());
            Map<String, Controller> currentHandler = controllers.get(handlerClass);

            for (Method method : handlerClass.getMethods()) {
                if (method.isAnnotationPresent(OnMessage.class)) {
                    OnMessage config = method.getAnnotation(OnMessage.class);
                    currentHandler.put(config.route(), new Controller(method, config.type()));
                }
            }
        }
    }

    private void setAuthGuards(Set<Class<? extends AuthGuard>> authGuards) {
        for (Class<? extends AuthGuard> authGuard : authGuards) {
            try {
                AuthGuard guard = (AuthGuard) injectDependencies(authGuard);
                SocketTransactionUtils.registerAuthGuard(guard);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                System.out.println("Could not instantiate authguards!");
                e.printStackTrace();
            }
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
                MessageBroker broker = new MessageBroker(client, ctx, controllers, this);

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
