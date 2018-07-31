package com.danielcs.socketserver.request;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

class HttpRequestBuilder {

    HttpURLConnection initConnection(String endpoint, String method) {
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod(method.toUpperCase());
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    String assembleUrl(String path, Map<String, String> params) {
        return path + "?" + encodeUri(params);
    }

    String assembleUrl(String path, Map<String, String> params, String fragment) {
        return assembleUrl(path, params) + "#" + encodeValue(fragment);
    }

    void setHeaders(HttpURLConnection conn, Map<String, String> headers) {
        headers.keySet().forEach(header -> conn.setRequestProperty(header, headers.get(header)));
    }

    void addToken(HttpURLConnection conn, String token) {
        conn.setRequestProperty("Authorization", "Bearer " + token);
    }

    String sendRequest(HttpURLConnection conn) {
        try {
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Server responded with statuscode: " + status);
            }
            String data = processResponse(conn);
            conn.disconnect();
            return data;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    private String processResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                conn.getInputStream()
        ));
        String input;
        StringBuilder sb = new StringBuilder();
        while ((input = reader.readLine()) != null) {
            sb.append(input);
        }
        return sb.toString();
    }

    private String encodeUri(Map<String, String> params) {
        return params.keySet().stream()
                .map(key -> encodeValue(key) + "=" + encodeValue(params.get(key)))
                .collect(Collectors.joining("&"));
    }

    private String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            System.out.println("UTF-8 not supported!");
        }
        return null;
    }
}
