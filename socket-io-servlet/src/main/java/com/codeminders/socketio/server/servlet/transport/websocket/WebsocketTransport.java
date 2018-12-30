/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p/>
 * Contributors: Ovea.com, Mycila.com
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.server.servlet.transport.websocket;

import com.codeminders.socketio.server.HttpRequest;
import com.codeminders.socketio.server.HttpResponse;
import com.codeminders.socketio.server.SocketIOManager;
import com.codeminders.socketio.server.TransportConnection;
import com.codeminders.socketio.server.TransportType;
import com.codeminders.socketio.server.servlet.transport.AbstractServletTransport;
import com.codeminders.socketio.server.transport.AbstractTransportConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public final class WebsocketTransport extends AbstractServletTransport
{
    private static final Logger LOGGER = Logger.getLogger(WebsocketTransport.class.getName());

    public WebsocketTransport(ServletConfig servletConfig, ServletContext servletContext) {
        super(servletConfig, servletContext);
    }

    @Override
    public TransportType getType()
    {
        return TransportType.WEB_SOCKET;
    }

    @Override
    public void handle(HttpRequest request,
                       HttpResponse response,
                       SocketIOManager sessionManager) throws IOException
    {

        if(!"GET".equals(request.getMethod()))
        {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "Only GET method is allowed for websocket transport");
            return;
        }

        if (request.getHeader("Sec-WebSocket-Key") == null) {

            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing request header 'Sec-WebSocket-Key'");
            return;
        }

        final TransportConnection connection = getConnection(request, sessionManager);

        // a bit hacky but safe since we know the type of TransportConnection here
        ((AbstractTransportConnection)connection).setRequest(request);
    }

    @Override
    public TransportConnection createConnection()
    {
        return new WebsocketTransportConnection(this);
    }
}
