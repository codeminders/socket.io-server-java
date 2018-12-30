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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Class to manage Socket.IO sessions and namespaces.
 */
public final class SocketIOManager
{
    private static final int SESSION_ID_LEN = 20;
    private static final char[] SYMBOLS;

    static
    {
        StringBuilder sb = new StringBuilder();
        for (char ch = 'A'; ch <= 'Z'; ch++)
            sb.append(ch);
        for (char ch = 'a'; ch <= 'z'; ch++)
            sb.append(ch);
        SYMBOLS = sb.toString().toCharArray();
    }

    private final Map<String, Namespace>         namespaces = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Session> sessions   = new ConcurrentHashMap<>();
    private TransportProvider transportProvider;

    final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private static SocketIOManager instance = new SocketIOManager();

    private SocketIOManager() {
    }

    public static SocketIOManager getInstance() {
        return instance;
    }

    private String generateSessionId()
    {
        while(true)
        {
            StringBuilder sb = new StringBuilder(SESSION_ID_LEN);
            for (int i = 0; i < SESSION_ID_LEN; i++)
                sb.append(SYMBOLS[ThreadLocalRandom.current().nextInt(SYMBOLS.length)]);

            String id = sb.toString();
            if(sessions.get(id) == null)
                return id;
        }
    }

    /**
     * Creates new session
     *
     * @return new session
     */
    public Session createSession()
    {
        Session session = new Session(this, generateSessionId());
        sessions.put(session.getSessionId(), session);
        return session;
    }

    /**
     * Finds existing session
     *
     * @param sessionId session id
     * @return session object or null if not found
     */
    public Session getSession(String sessionId)
    {
        return sessions.get(sessionId);
    }

    /**
     * Deletes the session
     *
     * @param sessionId session id
     */
    public void deleteSession(String sessionId)
    {
        sessions.remove(sessionId);
    }

    /**
     * Creates new namespace
     *
     * @param id namespace in. Should always start with '/'
     * @return new namespace
     */
    public Namespace createNamespace(String id)
    {
        Namespace ns = new Namespace(id);
        namespaces.put(ns.getId(), ns);
        return ns;
    }

    public Namespace getNamespace(String id)
    {
        return namespaces.get(id);
    }

    public TransportProvider getTransportProvider()
    {
        return transportProvider;
    }

    public void setTransportProvider(TransportProvider transportProvider)
    {
        this.transportProvider = transportProvider;
    }
}
