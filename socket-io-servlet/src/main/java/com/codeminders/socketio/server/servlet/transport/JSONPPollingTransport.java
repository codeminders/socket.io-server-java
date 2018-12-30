/**
 * The MIT License
 * Copyright (c) 2015
 *
 * Contributors: Tad Glines, Ovea.com, Mycila.com, Alexander Sova (bird@codeminders.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */
package com.codeminders.socketio.server.servlet.transport;

import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.server.HttpRequest;
import com.codeminders.socketio.server.HttpResponse;
import com.codeminders.socketio.server.Session;
import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.server.TransportType;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class JSONPPollingTransport extends AbstractHttpTransport
{
    private static final String EIO_PREFIX = "___eio";
    private static final String FRAME_ID   = JSONPPollingTransport.class.getName() + ".FRAME_ID";

    protected JSONPPollingTransport(ServletConfig servletConfig, ServletContext servletContext) {
        super(servletConfig, servletContext);
    }

    @Override
    public TransportType getType()
    {
        return TransportType.JSONP_POLLING;
    }

    public void startSend(Session session, HttpResponse response) throws IOException
    {
        response.setContentType("text/javascript; charset=UTF-8");
    }

    public void writeData(Session session, HttpResponse response, String data) throws IOException
    {
        StringBuilder sb = new StringBuilder()
                .append(EIO_PREFIX)
                .append("[").append(session.getAttribute(FRAME_ID)).append("]('")
                .append(data)
                .append("');");
        response.getOutputStream().write(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public void finishSend(Session session, HttpResponse response) throws IOException
    {
        response.flushBuffer();
    }

    public void onConnect(Session session, HttpRequest request, HttpResponse response)
            throws IOException
    {
        try {
            //TODO: Use string constant for request parameter name "j"
            //TODO: Do we really need to enforce "j" to be an integer?
            session.setAttribute(FRAME_ID, Integer.parseInt(request.getParameter(EngineIOProtocol.JSONP_INDEX)));
        } catch (NullPointerException | NumberFormatException e) {
            throw new SocketIOProtocolException("Missing or invalid 'j' parameter. It suppose to be integer");
        }

        startSend(session, response);
    }
}
