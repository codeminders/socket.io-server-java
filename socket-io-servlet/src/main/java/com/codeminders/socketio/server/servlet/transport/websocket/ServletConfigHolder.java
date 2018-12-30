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

import javax.servlet.ServletConfig;

/**
 * @author Alex Saveliev (lyolik@codeminders.com)
 */
public final class ServletConfigHolder
{
    private ServletConfig config;

    private static ServletConfigHolder instance = new ServletConfigHolder();

    private ServletConfigHolder() {
    }

    public static ServletConfigHolder getInstance() {
        return instance;
    }

    public void setConfig(ServletConfig config) {
        this.config = config;
    }

    public ServletConfig getConfig() {
        return this.config;
    }
}
