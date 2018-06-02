package com.danielcs.socketserver;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.danielcs.socketserver.Utils.decodeSocketStream;

class MessageBroker implements Runnable {

    private static final int BUFFER_SIZE = 4096;

    private final Socket socket;
    private final SocketContext context;
    private final Map<String, Handler> handlers = new HashMap<>();
    private final Gson converter = new Gson();
    private final MessageFormatter msgFormatter = new BasicMessageFormatter();  // TODO: make it a plugin

    public MessageBroker(Socket socket, SocketContext ctx, Map<Class, Map<String, Controller>> controllers) {
        this.socket = socket;
        this.context = ctx;
        initHandlers(controllers);
    }

    private void initHandlers(Map<Class,Map<String,Controller>> controllers) {
        try {
            for (Class handlerClass : controllers.keySet()) {
                Object instance = handlerClass.newInstance();
                Map<String, Controller> currentHandler = controllers.get(handlerClass);
                for (String route : currentHandler.keySet()) {
                    handlers.put(route, new Handler(
                            instance,
                            currentHandler.get(route).getMethod(),
                            currentHandler.get(route).getType()
                    ));
                }
            }
        } catch (IllegalAccessException | InstantiationException e) {
            System.out.println("Could not create controller object. HINT: it needs to have a default constructor!");
            System.exit(0);
        }
    }

    @Override
    public void run() {
        try (
                InputStream is = socket.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                OutputStream out = socket.getOutputStream()
        ) {

            boolean isConnectionValid = handleHandshake(in, out);
            if (!isConnectionValid) {
                System.out.println("Invalid handshake attempt was received. Thread broken.");
                return;
            }
            
            System.out.println("Listening for incoming messages...");
            byte[] stream = new byte[BUFFER_SIZE];
            int len;
            String msg;

            while (context.connected()) {
                len = is.read(stream);
                if (len != -1) {
                    msg = decodeSocketStream(stream, len);
                    if (msg == null || msg.equals("EOF")) {
                        break;
                    }
                    processMessage(msg);
                    // TODO: watch out for buffer overflow
                    stream = new byte[BUFFER_SIZE];
                }
            }
            System.out.println("Messagebroker stopped normally.");

        } catch (IOException e) {
            System.out.println("Messagebroker connection lost.");
        } finally {
            context.getUser().sendMessage("EOF");
        }
    }

    private boolean handleHandshake(BufferedReader in, OutputStream out) throws IOException {
        String msg = in.readLine();
        if (msg.startsWith("GET")) {
            // TODO: VOLATILE
            Pattern pattern = Pattern.compile("Sec-WebSocket-Key: (.*)");
            Matcher match = pattern.matcher(msg);
            boolean keyFound = match.find();
            while (!keyFound) {
                msg = in.readLine();
                match = pattern.matcher(msg);
                keyFound = match.find();
            }

            byte[] response;
            try {
                response = ("HTTP/1.1 101 Switching Protocols\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Sec-WebSocket-Accept: "
                        + DatatypeConverter.printBase64Binary(
                                MessageDigest
                                        .getInstance("SHA-1")
                                        .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                                .getBytes("UTF-8"))
                        )
                        + "\r\n\r\n").getBytes("UTF-8");
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                System.out.println("Could not encode handshake.");
                return false;
            }
            out.write(response, 0, response.length);
            return true;
        }
        return false;
    }

    private void processMessage(String msg) {
        // TODO: structural weakness, it should handle a nested object instead of special string
        try {
            msgFormatter.processMessage(msg);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        handlers.get(msgFormatter.getRoute()).handle(context, msgFormatter.getRawPayload());
    }

    private final class Handler {

        private final Object obj;
        private final Method method;
        private final Class type;

        Handler(Object obj, Method method, Class type) {
            this.obj = obj;
            this.method = method;
            this.type = type;
        }

        void handle(SocketContext context, String rawInput) {
            try {
                Object payload = type == String.class ? rawInput : converter.fromJson(rawInput, type);
                method.invoke(obj, context, payload);
            } catch (IllegalAccessException | InvocationTargetException e) {
                System.out.println("Handler call failed: " + method.getName());
            } catch (JsonSyntaxException ee) {
                System.out.println("JSON format was invalid.");
            }
        }
    }
}
