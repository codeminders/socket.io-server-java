package com.codeminders.socketio.server.transport.websocket;

import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.BinaryPacket;
import com.codeminders.socketio.protocol.EngineIOPacket;
import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.protocol.SocketIOPacket;
import com.codeminders.socketio.server.Config;
import com.codeminders.socketio.server.HttpRequest;
import com.codeminders.socketio.server.HttpResponse;
import com.codeminders.socketio.server.SocketIOProtocolException;
import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.transport.AbstractTransportConnection;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractWebsocketTransportConnection extends AbstractTransportConnection {
    private static final Logger LOGGER = Logger.getLogger(AbstractWebsocketTransportConnection.class.getName());

    private static final int SC_BAD_REQUEST = 400;

    protected AbstractWebsocketTransportConnection(Transport transport)
    {
        super(transport);
    }

    @Override
    protected void init()
    {
        getSession().setTimeout(getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT));

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(getConfig().getNamespace() + " WebSocket configuration:" +
                    " timeout=" + getSession().getTimeout());
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) throws IOException
    {
        response.sendError(SC_BAD_REQUEST, "Unexpected request on upgraded WebSocket connection");
    }

    @Override
    public void abort()
    {
        getSession().clearTimeout();
    }

    @Override
    public void send(EngineIOPacket packet) throws SocketIOException
    {
        sendString(EngineIOProtocol.encode(packet));
    }

    @Override
    public void send(SocketIOPacket packet) throws SocketIOException
    {
        send(EngineIOProtocol.createMessagePacket(packet.encode()));
        if(packet instanceof BinaryPacket)
        {
            Collection<InputStream> attachments = ((BinaryPacket) packet).getAttachments();
            for (InputStream is : attachments)
            {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                try
                {
                    os.write(EngineIOPacket.Type.MESSAGE.value());
                    ByteStreams.copy(is, os);
                }
                catch (IOException e)
                {
                    if(LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.SEVERE, "Cannot load binary object to send it to the socket", e);
                }
                sendBinary(os.toByteArray());
            }
        }
    }

    protected boolean sendHandshake() {
        if(getSession().getConnectionState() == ConnectionState.CONNECTING)
        {
            try
            {
                send(EngineIOProtocol.createHandshakePacket(getSession().getSessionId(),
                        new String[]{},
                        getConfig().getPingInterval(Config.DEFAULT_PING_INTERVAL),
                        getConfig().getTimeout(Config.DEFAULT_PING_TIMEOUT)));

                getSession().onConnect(this);
            }
            catch (SocketIOException e)
            {
                LOGGER.log(Level.SEVERE, "Cannot connect", e);
                getSession().setDisconnectReason(DisconnectReason.CONNECT_FAILED);
                return false;
            }
        }

        return true;
    }

    protected boolean handleTextFrame(String text) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Session[" + getSession().getSessionId() + "]: text received: " + text);

        getSession().resetTimeout();

        try
        {
            getSession().onPacket(EngineIOProtocol.decode(text), this);
        }
        catch (SocketIOProtocolException e)
        {
            if(LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Invalid packet received", e);
            return false;
        }

        return true;
    }

    protected boolean handleBinaryFrame(InputStream is) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Session[" + getSession().getSessionId() + "]: binary received");

        getSession().resetTimeout();

        try
        {
            getSession().onPacket(EngineIOProtocol.decode(is), this);
        }
        catch (SocketIOProtocolException e)
        {
            if(LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Problem processing binary received", e);
            return false;
        }

        return true;
    }

    protected void handleConnectionClosed(int closeCode, String closeReasonPhrase) {
        if(LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + getSession().getSessionId() + "]:" +
                    " websocket closed. (" + closeCode + "): " + closeReasonPhrase);

        //If close is unexpected then try to guess the reason based on closeCode, otherwise the reason is already set
        if(getSession().getConnectionState() != ConnectionState.CLOSING)
            getSession().setDisconnectReason(fromCloseCode(closeCode));

        getSession().setDisconnectMessage(closeReasonPhrase);
        getSession().onShutdown();
    }

    /**
     * @link https://tools.ietf.org/html/rfc6455#section-11.7
     */
    protected DisconnectReason fromCloseCode(int code)
    {
        switch (code) {
            case 1000:
                return DisconnectReason.CLOSED; // Normal Closure
            case 1001:
                return DisconnectReason.CLIENT_GONE; // Going Away
            default:
                return DisconnectReason.ERROR;
        }
    }

    protected abstract void sendString(String data) throws SocketIOException;

    protected abstract void sendBinary(byte[] data) throws SocketIOException;
}
