package com.codeminders.socketio.server;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpResponse {
    void setHeader(String name, String value);
    void setContentType(String contentType);
    OutputStream getOutputStream() throws IOException;
    void sendError(int statusCode, String message) throws IOException;
    void sendError(int statusCode) throws IOException;
    void flushBuffer() throws IOException;
}
