package org.spurint.socketio.server.akkahttp.transport

import akka.stream.Materializer
import com.codeminders.socketio.server.Transport
import com.codeminders.socketio.server.transport.AbstractTransportProvider
import org.spurint.socketio.server.akkahttp.SocketIOAkkaHttpSettings
import scala.concurrent.ExecutionContext

private[akkahttp] class AkkaHttpTransportProvider(settings: SocketIOAkkaHttpSettings)
                                                 (implicit ec: ExecutionContext,
                                                  mat: Materializer)
  extends AbstractTransportProvider
{
  override protected def createXHRPollingTransport(): Transport = new AkkaHttpXHRPollingTransport(settings)

  override protected def createJSONPPollingTransport(): Transport = null

  override protected def createWebSocketTransport(): Transport = new AkkaHttpWebSocketTransport(settings)
}
