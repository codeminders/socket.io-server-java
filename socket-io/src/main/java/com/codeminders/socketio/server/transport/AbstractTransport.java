package com.codeminders.socketio.server.transport;

import com.codeminders.socketio.protocol.EngineIOProtocol;
import com.codeminders.socketio.server.Config;
import com.codeminders.socketio.server.HttpRequest;
import com.codeminders.socketio.server.Session;
import com.codeminders.socketio.server.SocketIOManager;
import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.TransportConnection;

public abstract class AbstractTransport implements Transport  {
    protected abstract Config getConfig();

    private final TransportConnection createConnection(Session session)
    {
        TransportConnection connection = createConnection();
        connection.setSession(session);
        connection.init(getConfig());
        return connection;
    }

    protected TransportConnection getConnection(HttpRequest request, SocketIOManager sessionManager)
    {
        String sessionId = request.getParameter(EngineIOProtocol.SESSION_ID);
        Session session = null;

        if(sessionId != null && sessionId.length() > 0)
            session = sessionManager.getSession(sessionId);

        if(session == null)
            return createConnection(sessionManager.createSession());

        TransportConnection activeConnection = session.getConnection();

        if(activeConnection != null && activeConnection.getTransport() == this)
            return activeConnection;

        // this is new connection considered for an upgrade
        return createConnection(session);
    }

    @Override
    public String toString()
    {
        return getType().toString();
    }
}
