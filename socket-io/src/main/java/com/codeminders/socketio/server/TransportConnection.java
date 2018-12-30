/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 * Copyright (c) 2015 Alexander Sova (bird@codeminders.com)
 *
 * Contributors: Ovea.com, Mycila.com
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
 */
package com.codeminders.socketio.server;

import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.protocol.EngineIOPacket;
import com.codeminders.socketio.protocol.SocketIOPacket;

import java.io.IOException;

/**
 * @author Mathieu Carbou
 * @author Alexander Sova (bird@codeminders.com)
*/
public interface TransportConnection
{
    void init(Config config);
    void setSession(Session session);
    Session getSession();
    Transport getTransport();

    void handle(HttpRequest request, HttpResponse response)
            throws IOException;

    /**
     * Tears down the connection.
     */
    void abort();

    void send(EngineIOPacket packet) throws SocketIOException;
    void send(SocketIOPacket packet) throws SocketIOException;

    void disconnect(String namespace, boolean closeConnection);

    /**
     * Emits an event to the socket identified by the string name.
     *
     * @param namespace namespace
     * @param name event name
     * @param args list of arguments. Arguments can contain any type of field that can result of JSON decoding,
     *             including objects and arrays of arbitrary size.
     * @throws SocketIOException if IO or protocol error happens
     */

    void emit(String namespace, String name, Object... args) throws SocketIOException;

    /**
     * @return current HTTP request, null if connection is disconnected
     */
    HttpRequest getRequest();
}
