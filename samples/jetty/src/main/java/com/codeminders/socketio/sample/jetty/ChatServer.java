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
package com.codeminders.socketio.sample.jetty;

import com.codeminders.socketio.sample.chat.ChatSocketServlet;
import com.codeminders.socketio.server.servlet.transport.websocket.WebsocketTransportConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.JarResource;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class ChatServer {

    private static Resource getRoot() throws Exception {
        URL root = ChatServer.class.getClassLoader().getResource("");
        if (root != null) {
            return Resource.newResource(root);
        }
        root = ChatServer.class.getClassLoader().getResource("chat.html");
        if (root == null) {
            throw new IllegalArgumentException("Cannot identify static resources root");
        }
        if ("jar".equals(root.getProtocol())) {
            String path = root.getPath().substring(0, root.getPath().lastIndexOf("!/"));
            return JarResource.newJarResource(Resource.newResource(new File(URI.create(path))));
        } else {
            throw new IllegalArgumentException("Cannot identify static resources root");
        }
    }

    public static void main(String args[]) throws Exception {
        Server server = new Server(8080);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

        context.setContextPath("/");

        context.setBaseResource(getRoot());
        //context.setResourceBase();

        context.setWelcomeFiles(new String[]{"chat.html"});
        server.setHandler(context);
        context.addServlet(ChatSocketServlet.class, "/socket.io/*");
        context.addServlet(DefaultServlet.class, "/*");

        /*
         For root context endpoint initialization uses WebsocketTransportConnection annotations.
         Default endpoint configuration assumes it's accessible via /socket.io/ path
        */
        WebSocketServerContainerInitializer.
                configureContext(context).
                addEndpoint(WebsocketTransportConnection.class);

        /*
         For non-root contexts endpoint initialization should override endpoint path
         to refer to context-specific path. For example if context's path if /foo then
         we should declare path as "/bar" if endpoint is accessible via /foo/bar path.
         Default endpoint configuration assumes it's located in the root context and accessible via
         /socket.io/ path.
         See sample initialization code below
        */
        /*
        ServerContainer serverContainer = WebSocketServerContainerInitializer.
                configureContext(context);
        serverContainer.
                addEndpoint(new AnnotatedServerEndpointConfig(serverContainer,
                        WebsocketTransportConnection.class,
                        WebsocketTransportConnection.class.getAnnotation(ServerEndpoint.class),
                        null) {
                    @Override
                    public String getPath() {
                        return "/";
                    }
                });
         */

        server.start();
        server.join();
    }
}
