package com.danielcs.socketserver;

import java.io.UnsupportedEncodingException;

class Utils {

    static String decodeSocketStream(byte[] stream, int len) throws UnsupportedEncodingException {
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
        byte[] rawData = msg.getBytes();

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
}
