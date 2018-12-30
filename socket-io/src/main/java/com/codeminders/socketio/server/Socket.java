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
package com.codeminders.socketio.server;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public class Socket implements Outbound, DisconnectListener, EventListener
{
    private List<DisconnectListener>   disconnectListeners = new LinkedList<>();
    private Map<String, EventListener> eventListeners      = new LinkedHashMap<>();

    private Session session; // Socket is Session + Namespace
    private Namespace namespace;

    public Socket(Session session, Namespace namespace)
    {
        this.session   = session;
        this.namespace = namespace;
    }

    public String getNamespace()
    {
        return namespace.getId();
    }

    /**
     * Set listener for a named event. Only one listener per event is allowed.
     *
     * @param eventName event name
     * @param listener event listener
     */
    public void on(String eventName, EventListener listener)
    {
        eventListeners.put(eventName, listener);
    }

    /**
     * Closes socket.
     *
     * @param closeConnection closes underlying transport connection if true
     */
    public void disconnect(boolean closeConnection)
    {
        TransportConnection connection = getSession().getConnection();
        if (connection != null) {
            connection.disconnect(getNamespace(), closeConnection);
        }
    }

    @Override
    public void emit(String name, Object... args) throws SocketIOException
    {
        TransportConnection connection = getSession().getConnection();
        if (connection != null) {
            connection.emit(getNamespace(), name, args);
        }
    }

    /**
     * Adds disconnect listener
     *
     * @param listener disconnect listener
     */
    public void on(DisconnectListener listener)
    {
        disconnectListeners.add(listener);
    }

    public Session getSession()
    {
        return session;
    }

    @Override
    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
    {
        for (DisconnectListener listener : disconnectListeners)
            listener.onDisconnect(socket, reason, errorMessage);
    }

    @Override
    public Object onEvent(String name, Object[] args, boolean ackRequested)
    {
        EventListener listener = eventListeners.get(name);
        if(listener == null)
            return null;

        return listener.onEvent(name, args, ackRequested);
    }

    public void join(String room)
    {
        namespace.in(room).join(this);
    }

    public void leave(String room)
    {
        namespace.in(room).leave(this);
    }

    public void leaveAll()
    {
        namespace.leaveAll(this);
    }

    public void broadcast(String room, String name, Object... args)  throws SocketIOException
    {
        namespace.in(room).broadcast(this, name, args);
    }

    public String getId()
    {
        return getSession().getSessionId() + getNamespace();
    }

    /**
     * @return current HTTP request from underlying connection, null if socket is disconnected
     */
    public HttpRequest getRequest()
    {
        TransportConnection connection = getSession().getConnection();
        return connection == null ? null : connection.getRequest();
    }
}
