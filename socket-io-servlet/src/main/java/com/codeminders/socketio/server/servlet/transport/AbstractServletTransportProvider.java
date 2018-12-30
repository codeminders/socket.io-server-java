package com.codeminders.socketio.server.servlet.transport;

import com.codeminders.socketio.server.Transport;
import com.codeminders.socketio.server.transport.AbstractTransportProvider;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * @author Alexander Sova (bird@codeminders.com)
 */
public abstract class AbstractServletTransportProvider extends AbstractTransportProvider {
    private final ServletConfig servletConfig;
    private final ServletContext servletContext;

    protected AbstractServletTransportProvider(ServletConfig servletConfig, ServletContext servletContext) {
        this.servletConfig = servletConfig;
        this.servletContext = servletContext;
    }

    @Override
    protected Transport createXHRPollingTransport()
    {
        return new XHRPollingTransport(servletConfig, servletContext);
    }

    @Override
    protected Transport createJSONPPollingTransport()
    {
        return null;
    }

    @Override
    protected Transport createWebSocketTransport()
    {
        return null;
    }
}
