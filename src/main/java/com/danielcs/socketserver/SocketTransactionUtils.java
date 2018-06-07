package com.danielcs.socketserver;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SocketTransactionUtils {

    private static Method authGuard;

    static void setAuthGuard(Method authGuard) {
        SocketTransactionUtils.authGuard = authGuard;
    }

    static boolean authGuardPresent() {
        return authGuard != null;
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

    static boolean handleHandshake(InputStream in, OutputStream out) throws IOException {
        String msg = new Scanner(in,"UTF-8").useDelimiter("\\r\\n\\r\\n").next();
        if (msg.startsWith("GET")) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(msg);
            if (!match.find()) {
                return false;
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

    static boolean intercept(String token) {
        try {
            return (Boolean) authGuard.invoke(null, token);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("Authguard method could not be called!");
            return false;
        }
    }
}
