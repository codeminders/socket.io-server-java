package org.spurint.socketio.server.akkahttp.transport

import akka.http.scaladsl.model.StatusCodes
import com.codeminders.socketio.common.ConnectionState
import com.codeminders.socketio.protocol.EngineIOProtocol
import com.codeminders.socketio.server.{Config, TransportType, _}
import org.spurint.socketio.server.akkahttp.SocketIOAkkaHttpSettings

private[akkahttp] abstract class AkkaHttpPollingTransport(settings: SocketIOAkkaHttpSettings) extends AkkaHttpTransport(settings) {
  override def handle(request: HttpRequest, response: HttpResponse, socketIOManager: SocketIOManager): Unit = {
    val connection = getConnection(request, socketIOManager)
    val session = connection.getSession

    session.getConnectionState match {
      case ConnectionState.CONNECTING =>
        val upgrades = List(
          Option(socketIOManager.getTransportProvider.getTransport(TransportType.WEB_SOCKET))
            .map(_ => TransportType.WEB_SOCKET.toString)
        ).flatten

        connection.send(
          EngineIOProtocol.createHandshakePacket(
            session.getSessionId,
            upgrades.toArray,
            getConfig.getPingInterval(Config.DEFAULT_PING_INTERVAL),
            getConfig.getTimeout(Config.DEFAULT_PING_TIMEOUT)
          )
        )
        connection.handle(request, response) // called to send the handshake packet

        session.onConnect(connection)

      case ConnectionState.CONNECTED =>
        connection.handle(request, response)

      case _ =>
        response.sendError(StatusCodes.Gone.intValue, "Socket.IO session is closed")
    }
  }
}
