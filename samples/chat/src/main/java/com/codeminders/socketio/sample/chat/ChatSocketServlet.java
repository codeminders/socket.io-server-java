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
package com.codeminders.socketio.sample.chat;

import com.codeminders.socketio.common.DisconnectReason;
import com.codeminders.socketio.common.SocketIOException;
import com.codeminders.socketio.server.*;
import com.codeminders.socketio.server.servlet.transport.websocket.WebsocketIOServlet;
import com.google.common.io.ByteStreams;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatSocketServlet extends WebsocketIOServlet
{
    private static final String ANNOUNCEMENT     = "announcement";       // server to all connected clients
    private static final String CHAT_MESSAGE     = "chat message";       // broadcast to room
    private static final String WELCOME          = "welcome";            // single event sent by server to specific client
    private static final String FORCE_DISCONNECT = "force disconnect";   // client requests server to disconnect
    private static final String SERVER_BINARY    = "server binary";      // client requests server to send a binary
    private static final String CLIENT_BINARY    = "client binary";      // client sends binary

    private static final Logger LOGGER = Logger.getLogger(ChatSocketServlet.class.getName());

    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        of("/chat").on(new ConnectionListener()
        {
            @Override
            public void onConnect(final Socket socket)
            {
                try
                {
                    socket.emit(WELCOME, "Welcome to Socket.IO Chat, " + socket.getId() + "!");

                    socket.join("room");
                }
                catch (SocketIOException e)
                {
                    e.printStackTrace();
                    socket.disconnect(true);
                }

                socket.on(new DisconnectListener() {

                    @Override
                    public void onDisconnect(Socket socket, DisconnectReason reason, String errorMessage)
                    {
                        of("/chat").emit(ANNOUNCEMENT, socket.getSession().getSessionId() + " disconnected");
                    }
                });

                socket.on(CHAT_MESSAGE, new EventListener()
                {
                    @Override
                    public Object onEvent(String name, Object[] args, boolean ackRequested)
                    {
                        LOGGER.log(Level.FINE, "Received chat message: " + args[0]);

                        try
                        {
                            socket.broadcast("room", CHAT_MESSAGE, socket.getId(), args[0]);
                        }
                        catch (SocketIOException e)
                        {
                            e.printStackTrace();
                        }

                        return "OK"; //this object will be sent back to the client in ACK packet
                    }
                });

                socket.on(FORCE_DISCONNECT, new EventListener()
                {
                    @Override
                    public Object onEvent(String name, Object[] args, boolean ackRequested)
                    {
                        socket.disconnect(false);
                        return null;
                    }
                });

                socket.on(CLIENT_BINARY, new EventListener()
                {
                    @Override
                    public Object onEvent(String name, Object[] args, boolean ackRequested)
                    {
                        Map map = (Map<Object, Object>)args[0];
                        InputStream is = (InputStream) map.get("buffer");
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        try
                        {
                            ByteStreams.copy(is, os);
                            byte []array = os.toByteArray();
                            String s = "[";
                            for (byte b : array)
                                s += " " + b;
                            s += " ]";
                            LOGGER.log(Level.FINE, "Binary received: " + s);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }

                        return "OK";
                    }
                });

                socket.on(SERVER_BINARY, new EventListener()
                {
                    @Override
                    public Object onEvent(String name, Object[] args, boolean ackRequested)
                    {
                        try
                        {
                            socket.emit(SERVER_BINARY,
                                    new ByteArrayInputStream(new byte[] {1, 2, 3, 4}),
                                    new ACKListener()
                                    {
                                        @Override
                                        public void onACK(Object[] args)
                                        {
                                            System.out.println("ACK received: " + args[0]);
                                        }
                                    });
                        }
                        catch (SocketIOException e)
                        {
                            socket.disconnect(true);
                        }

                        return null;
                    }
                });
            }
        });

//        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                try
//                {
//                    of("/chat").in("room").emit("time", new Date().toString());
//                }
//                catch (SocketIOException e)
//                {
//                    e.printStackTrace();
//                }
//            }
//        }, 0, 20, TimeUnit.SECONDS);


//        of("/news").on(new ConnectionListener()
//        {
//            @Override
//            public void onConnect(Socket socket)
//            {
//                socket.on();
//            }
//        });
    }
}
