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
package com.codeminders.socketio.server;

import com.codeminders.socketio.common.SocketIOException;

import java.io.IOException;

public interface Transport
{
    /**
     * @return The name of the transport.
     */
    TransportType getType();

    /**
     * Initializes the transport
     *
     * @throws SocketIOException if init fails
     */
    void init()
        throws SocketIOException;

    void destroy();

    /**
     * Handles incoming HTTP request
     *
     * @param request object that contains the request the client made
     * @param response object that contains the response that will be returned to the client
     * @param socketIOManager session manager
     * @throws IOException if an input or output error occurs while the servlet is handling the request
     */
    void handle(HttpRequest request,
                HttpResponse response,
                SocketIOManager socketIOManager) throws IOException;

    /**
     * Creates new connection
     *
     * @return new transport connection
     */
    TransportConnection createConnection();
}
