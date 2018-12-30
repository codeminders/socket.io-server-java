/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 * <p>
 * Contributors: Ovea.com, Mycila.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.codeminders.socketio.server;

import com.codeminders.socketio.common.ConnectionState;
import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.ACKPacket;
import com.codeminders.socketio.protocol.BinaryPacket;
import com.codeminders.socketio.protocol.EngineIOPacket;
import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.protocol.EventPacket;
import com.codeminders.socketio.protocol.SocketIOPacket;
import com.codeminders.socketio.protocol.SocketIOProtocol;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO: this class is not thread-safe at all

/**
 * SocketIO session.
 * <p>
 * This implementation is not thread-safe.
 *
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Session implements DisconnectListener
{
    private static final Logger LOGGER = Logger.getLogger(Session.class.getName());

    private final SocketIOManager socketIOManager;
    private final String          sessionId;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private Map<String, Socket> sockets = new LinkedHashMap<>(); // namespace, socket

    private TransportConnection activeConnection;
    private ConnectionState  state            = ConnectionState.CONNECTING;

    private DisconnectReason disconnectReason = DisconnectReason.UNKNOWN;
    private String disconnectMessage;

    private long      timeout;
    private Future<?> timeoutTask;
    private boolean   timedOut;

    private BinaryPacket              binaryPacket;
    private int                       packet_id     = 0; // packet id. used for requesting ACK
    private Map<Integer, ACKListener> ack_listeners = new LinkedHashMap<>(); // packetid, listener

    Session(SocketIOManager socketIOManager, String sessionId)
    {
        assert (socketIOManager != null);

        this.socketIOManager = socketIOManager;
        this.sessionId = sessionId;
    }

    public Socket createSocket(String ns)
    {
        Namespace namespace = socketIOManager.getNamespace(ns);
        if (namespace == null)
            throw new IllegalArgumentException("Namespace does not exist");

        Socket socket = namespace.createSocket(this);
        socket.on(this); // listen for disconnect event
        sockets.put(ns, socket);
        return socket;
    }

    public void setAttribute(String key, Object val)
    {
        attributes.put(key, val);
    }

    public Object getAttribute(String key)
    {
        return attributes.get(key);
    }

    public String getSessionId()
    {
        return sessionId;
    }

    public ConnectionState getConnectionState()
    {
        return state;
    }

    public TransportConnection getConnection()
    {
        return activeConnection;
    }

    public void resetTimeout()
    {
        clearTimeout();
        if (timedOut || timeout == 0)
            return;

        timeoutTask = socketIOManager.executor.schedule(new Runnable()
        {
            @Override
            public void run()
            {
                Session.this.onTimeout();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public void clearTimeout()
    {
        if (timeoutTask != null)
        {
            timeoutTask.cancel(false);
            timeoutTask = null;
        }
    }

    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    public long getTimeout()
    {
        return timeout;
    }

    private void onBinary(InputStream is)
            throws SocketIOProtocolException
    {
        if (binaryPacket == null)
            throw new SocketIOProtocolException("Unexpected binary object");

        SocketIOProtocol.insertBinaryObject(binaryPacket, is);
        binaryPacket.addAttachment(is); //keeping copy of all attachments in attachments list
        if (binaryPacket.isComplete())
        {
            if (binaryPacket.getType() == SocketIOPacket.Type.BINARY_EVENT)
                onEvent((EventPacket) binaryPacket);
            else if (binaryPacket.getType() == SocketIOPacket.Type.BINARY_ACK)
                onACK((ACKPacket) binaryPacket);

            binaryPacket = null;
        }
    }

    public void onConnect(TransportConnection connection) throws SocketIOException
    {
        assert (connection != null);
        assert (this.activeConnection == null);

        this.activeConnection = connection;

        Socket socket = createSocket(SocketIOProtocol.DEFAULT_NAMESPACE);
        try
        {
            connection.send(SocketIOProtocol.createConnectPacket(SocketIOProtocol.DEFAULT_NAMESPACE));
            state = ConnectionState.CONNECTED;
            socketIOManager.getNamespace(SocketIOProtocol.DEFAULT_NAMESPACE).onConnect(socket); // callback
        }
        catch (ConnectionException e)
        {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Connection failed", e);

            connection.send(SocketIOProtocol.createErrorPacket(SocketIOProtocol.DEFAULT_NAMESPACE, e.getArgs()));
            closeConnection(DisconnectReason.CONNECT_FAILED, connection);
        }
    }

    /**
     * Optional. if transport knows detailed error message it could be set before calling onShutdown()
     *
     * @param message detailed explanation of the disconnect reason
     */
    public void setDisconnectMessage(String message)
    {
        this.disconnectMessage = message;
    }

    /**
     * Calling this method will change activeConnection status to CLOSING
     *
     * @param reason session disconnect reason
     */
    public void setDisconnectReason(DisconnectReason reason)
    {
        this.state = ConnectionState.CLOSING;
        this.disconnectReason = reason;
    }

    /**
     * callback to be called by transport activeConnection socket is closed.
     */
    public void onShutdown()
    {
        if (state == ConnectionState.CLOSING)
            onDisconnect(disconnectReason);
        else
            onDisconnect(DisconnectReason.ERROR);
    }

    /**
     * Disconnect callback. to be called by session itself. Transport activeConnection should always call onShutdown()
     */
    private void onDisconnect(DisconnectReason reason)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onDisconnect: " + reason +
                    " message: [" + disconnectMessage + "]");

        if (state == ConnectionState.CLOSED)
            return; // to prevent calling it twice

        state = ConnectionState.CLOSED;

        clearTimeout();

        // taking copy of sockets because
        // session will be modifying the collection while iterating
        for (Object o : sockets.values().toArray())
        {
            Socket socket = (Socket) o;
            socket.onDisconnect(socket, reason, disconnectMessage);
        }

        socketIOManager.deleteSession(sessionId);
    }

    private void onTimeout()
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Session[" + sessionId + "]: onTimeout");

        if (!timedOut)
        {
            timedOut = true;
            closeConnection(DisconnectReason.TIMEOUT, activeConnection);
        }
    }

    public void onPacket(EngineIOPacket packet, TransportConnection connection)
    {
        switch (packet.getType())
        {
            case OPEN:
            case PONG:
                // ignore. OPEN and PONG are server -> client only
                return;

            case MESSAGE:
                resetTimeout();
                try
                {
                    if (packet.getTextData() != null)
                        onPacket(SocketIOProtocol.decode(packet.getTextData()));
                    else if (packet.getBinaryData() != null)
                        onBinary(packet.getBinaryData());
                }
                catch (SocketIOProtocolException e)
                {
                    if (LOGGER.isLoggable(Level.WARNING))
                        LOGGER.log(Level.WARNING, "Invalid SIO packet: " + packet.getTextData(), e);
                }
                return;

            case PING:
                resetTimeout();
                onPing(packet.getTextData(), connection);

                // ugly hack to replicate current sio client behaviour
                if (connection != getConnection())
                    forcePollingCycle();

                return;

            case CLOSE:
                closeConnection(DisconnectReason.CLOSED_REMOTELY, connection);
                return;

            case UPGRADE:
                upgradeConnection(connection);
                return;

            default:
                throw new UnsupportedOperationException("EIO Packet " + packet + " is not implemented yet");

        }
    }

    private void onPacket(SocketIOPacket packet)
    {
        switch (packet.getType())
        {
            case CONNECT:
                try
                {
                    if (socketIOManager.getNamespace(packet.getNamespace()) == null)
                    {
                        getConnection().send(SocketIOProtocol.createErrorPacket(packet.getNamespace(), "Invalid namespace"));
                        return;
                    }
                    Socket socket = createSocket(packet.getNamespace());
                    getConnection().send(SocketIOProtocol.createConnectPacket(packet.getNamespace()));
                    try
                    {
                        socketIOManager.getNamespace(socket.getNamespace()).onConnect(socket);
                    }
                    catch (ConnectionException e)
                    {
                        getConnection().send(SocketIOProtocol.createErrorPacket(socket.getNamespace(), e.getArgs()));
                        socket.disconnect(false);
                    }
                }
                catch (SocketIOException e)
                {
                    if (LOGGER.isLoggable(Level.FINE))
                        LOGGER.log(Level.FINE, "Cannot send packet to the client", e);

                    closeConnection(DisconnectReason.CONNECT_FAILED, activeConnection);
                }
                return;

            case DISCONNECT:
                closeConnection(DisconnectReason.CLOSED_REMOTELY, activeConnection);
                return;

            case EVENT:
                onEvent((EventPacket) packet);
                return;

            case ACK:
                onACK((ACKPacket) packet);
                return;

            case BINARY_ACK:
            case BINARY_EVENT:
                binaryPacket = (BinaryPacket) packet;
                return;

            default:
                throw new UnsupportedOperationException("SocketIO packet " + packet.getType() + " is not implemented yet");
        }
    }

    private void onPing(String data, TransportConnection connection)
    {
        try
        {
            connection.send(EngineIOProtocol.createPongPacket(data));
        }
        catch (SocketIOException e)
        {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "connection.send failed: ", e);

            closeConnection(DisconnectReason.ERROR, connection);
        }
    }

    private void onEvent(EventPacket packet)
    {
        if (state != ConnectionState.CONNECTED)
            return;

        try
        {
            Namespace ns = socketIOManager.getNamespace(packet.getNamespace());
            if (ns == null)
            {
                getConnection().send(SocketIOProtocol.createErrorPacket(packet.getNamespace(), "Invalid namespace"));
                return;
            }

            Socket socket = sockets.get(ns.getId());
            if (socket == null)
            {
                activeConnection.send(SocketIOProtocol.createErrorPacket(packet.getNamespace(),
                        "No socket is connected to the namespace"));
                return;
            }

            Object ack = socket.onEvent(packet.getName(), packet.getArgs(), packet.getId() != -1);

            if (packet.getId() != -1 && ack != null)
            {
                Object[] args;
                if (ack instanceof Objects[])
                    args = (Object[]) ack;
                else
                    args = new Object[]{ack};

                activeConnection.send(SocketIOProtocol.createACKPacket(packet.getId(), packet.getNamespace(), args));
            }
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by one of the event listeners", e);
        }
    }

    private void onACK(ACKPacket packet)
    {
        if (state != ConnectionState.CONNECTED)
            return;

        try
        {
            ACKListener listener = ack_listeners.get(packet.getId());
            unsubscribeACK(packet.getId());
            if (listener != null)
                listener.onACK(packet.getArgs());
        }
        catch (Throwable e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Session[" + sessionId + "]: Exception thrown by ACK listener", e);
        }
    }

    private void upgradeConnection(TransportConnection connection)
    {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Upgrading from " + this.activeConnection.getTransport() + " to " + connection.getTransport());

        this.activeConnection = connection;
    }

    /**
     * Remembers the disconnect reason and closes underlying transport activeConnection
     */
    private void closeConnection(DisconnectReason reason, TransportConnection connection)
    {
        if (this.activeConnection == connection)
            setDisconnectReason(reason);
        connection.abort(); //this call should trigger onShutdown() eventually
    }

    public synchronized int getNewPacketId()
    {
        return packet_id++;
    }

    //TODO: what if ACK never comes? We will have a memory leak. Need to cleanup the list or fail on timeout?
    public void subscribeACK(int packet_id, ACKListener ack_listener)
    {
        ack_listeners.put(packet_id, ack_listener);
    }

    public void unsubscribeACK(int packet_id)
    {
        ack_listeners.remove(packet_id);
    }

    @Override
    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
    {
        sockets.remove(socket.getNamespace());
    }

    // hack to replicate current Socket.IO client behaviour
    private void forcePollingCycle()
    {
        try
        {
            TransportConnection connection = getConnection();
            if (connection != null) {
                connection.send(EngineIOProtocol.createNoopPacket());
            }
        }
        catch (SocketIOException e)
        {
            if (LOGGER.isLoggable(Level.WARNING))
                LOGGER.log(Level.WARNING, "Cannot send NOOP packet while upgrading the transport", e);
        }
    }
}
