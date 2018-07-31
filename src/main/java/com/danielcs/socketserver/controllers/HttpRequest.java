package com.danielcs.socketserver.controllers;

import com.danielcs.socketserver.annotations.HttpRequestAssembler;

import java.util.Map;

@HttpRequestAssembler
public interface HttpRequest {
    String getWithQuery(String url, Map<String, String> queryParams);
}
