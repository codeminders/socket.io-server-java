/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
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
package com.codeminders.socketio.server.servlet;

import com.codeminders.socketio.server.Config;

import javax.servlet.ServletConfig;

/**
 * @author Mathieu Carbou
 */
public final class ServletBasedConfig implements Config
{
    private final ServletConfig config;
    private final String        namespace;

    public ServletBasedConfig(ServletConfig config, String namespace)
    {
        this.namespace = namespace;
        this.config = config;
    }

    @Override
    public long getPingInterval(long def)
    {
        return getLong(PING_INTERVAL, def);
    }

    @Override
    public long getTimeout(long def)
    {
        return getLong(TIMEOUT, def);
    }

    @Override
    public int getBufferSize()
    {
        return getInt(BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public int getMaxIdle()
    {
        return getInt(MAX_IDLE, DEFAULT_MAX_IDLE);
    }

    @Override
    public int getInt(String param, int def)
    {
        String v = getString(param);
        return v == null ? def : Integer.parseInt(v);
    }

    @Override
    public long getLong(String param, long def)
    {
        String v = getString(param);
        return v == null ? def : Long.parseLong(v);
    }

    @Override
    public boolean getBoolean(String key, boolean def)
    {
        String v = getString(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    @Override
    public String getNamespace()
    {
        return namespace;
    }

    @Override
    public String getString(String param)
    {
        String v = config.getInitParameter(namespace + "." + param);
        if (v == null)
            v = config.getInitParameter(param);

        return v;
    }

    @Override
    public String getString(String param, String def)
    {
        String v = getString(param);
        return v == null ? def : v;
    }

}
