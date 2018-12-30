Java/Scala backend for `Socket.IO` library (http://socket.io/)

Supports `Socket.IO` clients version 1.0+

There is currently support for JSR 356-compatible servlet containers (tested with Jetty 9 and Tomcat 8) and Akka-HTTP.

Right now only websocket and XHR polling transports are implemented.

Based on https://github.com/tadglines/Socket.IO-Java

License: MIT

## Websocket endpoint initialization in Jetty

Default websocket endpoint configuration assumes it's located in the root context and accessible via `/socket.io/` path.

When `Socket.IO` backend is integrated into webapp managed by Jetty there is no need to perform additional configuration because Jetty scans for `@ServerEndpoint` annotation and initializes websocket endpoint automatically.

When Jetty server is embedded into your application, websocket endpoint is located in the root context ("/") and expected to be accessible via `/socket.io/` path (default configuration), then in order to initialize endpoint you should add the following code 

```java
        WebSocketServerContainerInitializer.
                configureContext(context).
                addEndpoint(WebsocketTransportConnection.class);

```

When Jetty server is embedded into your application, but websocket endpoint is either located not in the root context ("/") or  expected to be accessible via path other than `/socket.io/`, then in order to initialize endpoint you should add the following code 

```java
        ServerContainer serverContainer = WebSocketServerContainerInitializer.
                configureContext(context);
        serverContainer.
                addEndpoint(new AnnotatedServerEndpointConfig(serverContainer,
                        WebsocketTransportConnection.class,
                        WebsocketTransportConnection.class.getAnnotation(ServerEndpoint.class),
                        null) {
                    @Override
                    public String getPath() {
                        return "/"; // context-relative path, "/bar" for context "/foo" and path "/foo/bar"
                    }
                });
```
See example in [com.codeminders.socketio.sample.jetty.ChatServer](https://github.com/codeminders/socket.io-server-java/blob/master/samples/jetty/src/main/java/com/codeminders/socketio/sample/jetty/ChatServer.java)

## Akka-HTTP usage

The akka-http support revolves around a single exposed class, `SocketIOAkkaHttp`.

```scala
        val socketIO = SocketIOAkkaHttp().fold(sys.error, identity)

        val routes: Route = {
            pathPrefix("socket.io") {
                socketIO.route
            }
        }

        Http().bindAndHandle(routes, "localhost", 8080)
```

The socket URL can be changed by moving the `.route` call to another area in your routes hierarchy.  To configure limits and timeouts, an optional `SocketIOAkkaHttpSettings` instance can be passed to the `SocketIOAkkaHttp()` constructor.
