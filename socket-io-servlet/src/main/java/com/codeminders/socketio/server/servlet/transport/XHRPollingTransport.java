/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
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
package com.codeminders.socketio.server.servlet.transport;

import com.codeminders.socketio.server.TransportConnection;
import com.codeminders.socketio.server.TransportType;
import com.codeminders.socketio.server.transport.XHRTransportConnection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

public class XHRPollingTransport extends AbstractHttpTransport
{
    public XHRPollingTransport(ServletConfig servletConfig, ServletContext servletContext) {
        super(servletConfig, servletContext);
    }

    @Override
    public TransportType getType()
    {
        return TransportType.XHR_POLLING;
    }

    @Override
    public TransportConnection createConnection()
    {
        return new XHRTransportConnection(this);
    }

}
