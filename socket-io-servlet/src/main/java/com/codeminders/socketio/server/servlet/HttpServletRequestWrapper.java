package com.codeminders.socketio.server.servlet;

import com.codeminders.socketio.server.HttpRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

public class HttpServletRequestWrapper implements HttpRequest {
    private final HttpServletRequest request;

    public HttpServletRequestWrapper(HttpServletRequest request)
    {
        this.request = request;
    }

    @Override
    public String getMethod()
    {
        return request.getMethod();
    }

    @Override
    public String getHeader(String name)
    {
        return request.getHeader(name);
    }

    @Override
    public String getContentType()
    {
        return request.getContentType();
    }

    @Override
    public String getParameter(String name)
    {
        return request.getParameter(name);
    }

    @Override
    public InputStream getInputStream()
            throws IOException
    {
        return request.getInputStream();
    }

    @Override
    public BufferedReader getReader()
            throws IOException
    {
        return request.getReader();
    }
}
