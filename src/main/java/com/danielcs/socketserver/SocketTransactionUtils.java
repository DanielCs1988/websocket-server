package com.danielcs.socketserver;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SocketTransactionUtils {

    private static List<AuthGuard> authGuards = new ArrayList<>();

    static boolean authGuardPresent() {
        return !authGuards.isEmpty();
    }

    static void registerAuthGuard(AuthGuard authGuard) {
        authGuards.add(authGuard);
    }

    static String decodeSocketStream(byte[] stream, int len) throws UnsupportedEncodingException {
        if (stream[0] == -120) {
            return null;
        }

        String msg;
        int rMaskIndex = 2;
        byte data = stream[1];
        byte op = (byte)127;
        byte rLength = (byte)(data & op);

        if (rLength == (byte) 126) {
            rMaskIndex = 4;
        } else if (rLength == (byte) 127) {
            rMaskIndex = 10;
        }

        byte[] masks = new byte[4];
        System.arraycopy(stream, rMaskIndex, masks, 0, 4);

        int rDataStart = rMaskIndex + 4;
        int messLen = len - rDataStart;
        byte[] message = new byte[messLen];

        for (int i = 0; i < messLen; i++) {
            message[i] = (byte)(stream[i + rDataStart] ^ masks[i % 4]);
        }

        msg = new String(message, "UTF-8");
        return msg;
    }

    @SuppressWarnings("ShiftOutOfRange")
    static byte[] encodeSocketStream(String msg) {
        byte[] rawData = new byte[0];
        try {
            rawData = msg.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Could not encode stream to UTF-8!");
            rawData = msg.getBytes();
        }

        int frameCount;
        byte[] frame = new byte[10];
        frame[0] = (byte)129;

        if (rawData.length <= 125){
            frame[1] = (byte)rawData.length;
            frameCount = 2;
        } else if (rawData.length <= 65535){
            frame[1] = (byte)126;
            int len = rawData.length;
            frame[2] = (byte)((len >> 8 ) & (byte)255);
            frame[3] = (byte)(len & (byte)255);
            frameCount = 4;
        } else {
            frame[1] = (byte)127;
            int len = rawData.length;
            frame[2] = (byte)((len >> 56 ) & (byte)255);
            frame[3] = (byte)((len >> 48 ) & (byte)255);
            frame[4] = (byte)((len >> 40 ) & (byte)255);
            frame[5] = (byte)((len >> 32 ) & (byte)255);
            frame[6] = (byte)((len >> 24 ) & (byte)255);
            frame[7] = (byte)((len >> 16 ) & (byte)255);
            frame[8] = (byte)((len >> 8 ) & (byte)255);
            frame[9] = (byte)(len & (byte)255);
            frameCount = 10;
        }

        int bLength = frameCount + rawData.length;
        byte[] reply = new byte[bLength];
        System.arraycopy(frame, 0, reply, 0, frameCount);
        System.arraycopy(rawData, 0, reply, frameCount, rawData.length);
        return reply;
    }

    static boolean handleHandshake(InputStream in, OutputStream out, SocketContext ctx) throws IOException {
        String msg = new Scanner(in,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();
        boolean isGetRequest = msg.startsWith("GET");
        if (!(isGetRequest && validateAuthToken(msg, ctx))) {
            byte[] reason = isGetRequest ? "HTTP/1.1 401".getBytes() : "HTTP/1.1 400".getBytes();
            out.write(reason, 0, reason.length);
            return false;
        }

        Matcher keyMatcher = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(msg);
        keyMatcher.find();

        byte[] response;
        try {
            response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + DatatypeConverter.printBase64Binary(
                    MessageDigest
                            .getInstance("SHA-1")
                            .digest((keyMatcher.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
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

    private static boolean validateAuthToken(String msg, SocketContext ctx) {
        if (!authGuardPresent()) {
            return true;
        }
        Matcher authTokenMatcher = Pattern.compile("GET /(.+) HTTP").matcher(msg);
        if (!authTokenMatcher.find()) {
            System.out.println("Authorization token not found!");
            return false;
        }
        String token = authTokenMatcher.group(1);
        if (!intercept(token, ctx)) {
            System.out.println("Authorization token is invalid!");
            return false;
        }
        return true;
    }

    static boolean intercept(String token, SocketContext ctx) {
        for (AuthGuard authGuard : authGuards) {
            if (!authGuard.authorize(ctx, token)) {
                return false;
            }
        }
        return true;
    }
}
