package com.codeminders.socketio.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public interface HttpRequest {
    String getMethod();
    String getHeader(String name);
    String getContentType();
    String getParameter(String name);
    InputStream getInputStream() throws IOException;
    BufferedReader getReader() throws IOException;
}
