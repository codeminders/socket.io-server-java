package org.spurint.socketio.server.akkahttp.transport
import akka.stream.Materializer
import com.codeminders.socketio.server._
import org.spurint.socketio.server.akkahttp.SocketIOAkkaHttpSettings
import scala.concurrent.ExecutionContext

private[akkahttp] class AkkaHttpWebSocketTransport(settings: SocketIOAkkaHttpSettings)
                                                  (implicit ec: ExecutionContext,
                                                   mat: Materializer)
  extends AkkaHttpTransport(settings)
{
  override def init(): Unit = ()

  override def destroy(): Unit = ()

  override def getType: TransportType = TransportType.WEB_SOCKET

  override def handle(request: HttpRequest, response: HttpResponse, socketIOManager: SocketIOManager): Unit = {
    getConnection(request, socketIOManager) match {
      case connection: AkkaHttpWebSocketConnection =>
        connection.setRequest(request)
      case connection =>
        connection.abort()
    }
    // the caller will handle starting up the web socket connection and returning an apprpriate response
  }

  override def createConnection(): TransportConnection = new AkkaHttpWebSocketConnection(this)

  override def getConnection(request: HttpRequest, sessionManager: SocketIOManager): TransportConnection = {
    super.getConnection(request, sessionManager)
  }
}
