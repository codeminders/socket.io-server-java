package org.spurint.socketio.server.akkahttp.transport

import com.codeminders.socketio.server.transport.XHRTransportConnection
import com.codeminders.socketio.server.{TransportConnection, TransportType}
import org.spurint.socketio.server.akkahttp.SocketIOAkkaHttpSettings

private[akkahttp] class AkkaHttpXHRPollingTransport(settings: SocketIOAkkaHttpSettings) extends AkkaHttpPollingTransport(settings) {
  override def init(): Unit = ()

  override def destroy(): Unit = ()

  override def getType: TransportType = TransportType.XHR_POLLING

  override def createConnection(): TransportConnection = new XHRTransportConnection(this)

}
