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
package com.codeminders.socketio.server;

/**
 * @author Mathieu Carbou
 */
public interface Config
{
    String MAX_TEXT_MESSAGE_SIZE = "maxTextMessageSize";
    String PING_INTERVAL         = "pingInterval";
    String TIMEOUT               = "timeout";

    String BUFFER_SIZE = "bufferSize";
    String MAX_IDLE    = "maxIdleTime";

    String WEBSOCKET_SEND_SYNCHRONIZED = "websocketSendSynchronized";

    int DEFAULT_BUFFER_SIZE = 8192;
    int DEFAULT_MAX_IDLE    = 300 * 1000;

    int DEFAULT_PING_INTERVAL = 25 * 1000; // 25s
    int DEFAULT_PING_TIMEOUT  = 60 * 1000; // 60s

    boolean DEFAULT_WEBSOCKET_SEND_SYNCHRONIZED = false;

    long getPingInterval(long def);

    long getTimeout(long def);

    int getBufferSize();

    int getMaxIdle();

    String getString(String key);

    String getString(String key, String def);

    int getInt(String key, int def);

    long getLong(String key, long def);

    boolean getBoolean(String key, boolean def);

    String getNamespace();

    boolean isWebsocketSendSynchronized();
}
