package com.codeminders.socketio.server.servlet;

import com.codeminders.socketio.server.HttpResponse;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class HttpServletResponseWrapper implements HttpResponse {
    private final HttpServletResponse response;

    public HttpServletResponseWrapper(HttpServletResponse response)
    {
        this.response = response;
    }

    @Override
    public void setHeader(String name, String value)
    {
        response.setHeader(name, value);
    }

    @Override
    public void setContentType(String contentType)
    {
        response.setContentType(contentType);
    }

    @Override
    public OutputStream getOutputStream()
            throws IOException
    {
        return response.getOutputStream();
    }

    @Override
    public void sendError(int statusCode, String message)
            throws IOException
    {
        response.sendError(statusCode, message);
    }

    @Override
    public void sendError(int statusCode)
            throws IOException
    {
        response.sendError(statusCode);
    }

    @Override
    public void flushBuffer()
            throws IOException
    {
        response.flushBuffer();
    }
}
