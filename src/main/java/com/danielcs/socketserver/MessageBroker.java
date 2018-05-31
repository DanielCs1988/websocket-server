package com.danielcs.socketserver;

import com.google.gson.Gson;

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

public class MessageBroker implements Runnable {

    private static final int BUFFER_SIZE = 4096;
    static final String SEPARATOR = "×";
    private final Socket socket;
    private final SocketServer.EmitterImpl emitterImpl;
    private Map<String, Handler> handlers = new HashMap<>();
    private Gson converter = new Gson();

    public MessageBroker(Socket socket, SocketServer.EmitterImpl emitterImpl, Map<Class, Map<String, Controller>> controllers) {
        this.socket = socket;
        this.emitterImpl = emitterImpl;
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
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader in = new BufferedReader(isr);
                OutputStream out = socket.getOutputStream()
        ) {

            boolean isConnectionValid = handleHandshake(in, out);
            if (!isConnectionValid) {
                System.out.println("Invalid handshake attempt was received. Thread broken.");
                emitterImpl.reply("EOF", null);
                return;
            }
            
            System.out.println("Listening for incoming messages...");
            byte[] stream = new byte[BUFFER_SIZE];
            int len;
            String msg;

            while (true) {
                len = is.read(stream);
                if (len != -1) {
                    msg = decodeSocketStream(stream, len);
                    if (msg.equals("EOF")) {
                        break;
                    }
                    System.out.println(msg);
                    processMessage(msg);
                    stream = new byte[BUFFER_SIZE];
                }
            }
            System.out.println("Messagebroker stopped normally.");

        } catch (IOException e) {
            System.out.println("Messagebroker connection lost.");
            System.out.println(socket.isClosed());
            e.printStackTrace();
        } finally {
            // TODO: make it nicer
            emitterImpl.reply("EOF", null);
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
        String[] fullMsg = msg.split(SEPARATOR);
        if (fullMsg.length != 2) {
            System.out.println("Received a faulty message.");
            return;
        }
        String route = fullMsg[0];
        String payload = fullMsg[1];
        handlers.get(route).handle(emitterImpl, payload);
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

        void handle(SocketServer.EmitterImpl emitterImpl, String rawInput) {
            // TODO: wrong input needs to be handled
            Object payload = type == String.class ? rawInput : converter.fromJson(rawInput, type);
            try {
                method.invoke(obj, emitterImpl, payload);
            } catch (IllegalAccessException | InvocationTargetException e) {
                System.out.println("Handler call failed: " + method.getName());
            }
        }
    }
}
