/**
 * The MIT License
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
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

import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.server.Config;
import com.codeminders.socketio.server.SocketIOManager;
import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.servlet.ServletBasedConfig;
import com.codeminders.socketio.server.transport.websocket.AbstractWebsocketTransportConnection;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Alexander Sova (bird@codeminders.com)
 * @author Alex Saveliev (lyolik@codeminders.com)
 */
@ServerEndpoint(value="/socket.io/", configurator = WebsocketConfigurator.class)
public final class WebsocketTransportConnection extends AbstractWebsocketTransportConnection
{
    private static final Logger LOGGER = Logger.getLogger(WebsocketTransportConnection.class.getName());

    private static Class<? extends WebsocketIO> websocketIOClass = WebsocketIO.class;

    private WebsocketIO websocketIO;

    public WebsocketTransportConnection(Transport transport)
    {
        super(transport);
    }

    /**
     *
     * @param clazz class responsible for I/O operations
     */
    public static void setWebsocketIOClass(Class<? extends WebsocketIO> clazz) {
        WebsocketTransportConnection.websocketIOClass = clazz;
    }

    @OnOpen
    public void onOpen(javax.websocket.Session session, EndpointConfig config) throws Exception
    {
        setupIO(session);
        setupSession(session);
        init(new ServletBasedConfig(
                ServletConfigHolder.getInstance().getConfig(),
                getTransport().getType().toString()));
        session.setMaxBinaryMessageBufferSize(getConfig().getBufferSize());
        session.setMaxIdleTimeout(getConfig().getMaxIdle());
        session.setMaxTextMessageBufferSize(getConfig().getInt(Config.MAX_TEXT_MESSAGE_SIZE, 32000));
        sendHandshake();
    }

    private void setupIO(Session session) throws Exception {
        websocketIO = websocketIOClass.getConstructor(Session.class).newInstance(session);
    }

    @OnClose
    public void onClose(javax.websocket.Session session, CloseReason closeReason)
    {
        handleConnectionClosed(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
    }

    @OnMessage
    public void onMessage(String text)
    {
        handleTextFrame(text);
    }

    @OnMessage
    public void onMessage(InputStream is)
    {
        handleBinaryFrame(is);
    }

    @OnError
    public void onError(javax.websocket.Session session, Throwable error) {
        if (websocketIO != null)
        {
            disconnectEndpoint();
            websocketIO = null;
        }
    }

    @Override
    public void abort()
    {
        super.abort();
        if (websocketIO != null)
        {
            disconnectEndpoint();
            websocketIO = null;
        }
    }

    protected void sendString(String data) throws SocketIOException
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]: send text: " + data);

        try
        {
            websocketIO.sendString(data);
        }
        catch (IOException e)
        {
            disconnectEndpoint();
            throw new SocketIOException(e);
        }
    }

    //TODO: implement streaming. right now it is all in memory.
    //TODO: read and send in chunks using sendPartialBytes()
    protected void sendBinary(byte[] data) throws SocketIOException
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]: send binary");

        try
        {
            websocketIO.sendBinary(data);
        }
        catch (IOException e)
        {
            disconnectEndpoint();
            throw new SocketIOException(e);
        }
    }

    private void disconnectEndpoint()
    {
        try
        {
            websocketIO.disconnect();
        }
        catch (IOException ex)
        {
            // ignore
        }
    }

    /**
     * @param session websocket session
     * @return session id extracted from handshake request's parameter
     */
    private String getSessionId(javax.websocket.Session session)
    {
        HandshakeRequest handshake = (HandshakeRequest)
                session.getUserProperties().get(HandshakeRequest.class.getName());
        if (handshake == null) {
            return null;
        }
        List<String> values = handshake.getParameterMap().get(EngineIOProtocol.SESSION_ID);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    /**
     * Initializes socket.io session
     * @param session websocket session
     */
    private void setupSession(javax.websocket.Session session)
    {
        String sessionId = getSessionId(session);
        com.codeminders.socketio.server.Session sess = null;
        if (sessionId != null) {
            sess = SocketIOManager.getInstance().getSession(sessionId);
        }
        if (sess == null) {
            sess = SocketIOManager.getInstance().createSession();
        }
        setSession(sess);
    }
}
