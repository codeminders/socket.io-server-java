package org.spurint.socketio.server.akkahttp

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.UpgradeToWebSocket
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.StreamConverters
import com.codeminders.socketio.common.SocketIOException
import com.codeminders.socketio.protocol.{EngineIOProtocol, SocketIOProtocol}
import com.codeminders.socketio.server.{Namespace, SocketIOManager, UnsupportedTransportException}
import org.spurint.socketio.server.akkahttp.transport.{AkkaHttpTransportProvider, AkkaHttpWebSocketConnection, AkkaHttpWebSocketTransport}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

object SocketIOAkkaHttp {
  /**
    * Creates a new [[SocketIOAkkaHttp]] object.
    *
    * This is the main entry point for embedding a Socket.IO server in an akka-http server.
    *
    * @param settings an (optional) [[SocketIOAkkaHttpSettings]] instance
    * @param ec an [[ExecutionContext]]
    * @param system an [[ActorSystem]]
    * @param mat a [[Materializer]]
    * @return a [[Right]] containing a new [[SocketIOAkkaHttp]] instance, or a [[Left]] of [[String]] containing an error message
    */
  def apply(settings: SocketIOAkkaHttpSettings)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Either[String, SocketIOAkkaHttp] = {
    try {
      Right(new SocketIOAkkaHttp(settings))
    } catch {
      case NonFatal(e) => Left(e.getMessage)
    }
  }

  /**
    * Creates a new [[SocketIOAkkaHttp]] object.
    *
    * This is the main entry point for embedding a Socket.IO server in an akka-http server.
    *
    * Default settings are applied.
    *
    * @param ec an [[ExecutionContext]]
    * @param system an [[ActorSystem]]
    * @param mat a [[Materializer]]
    * @return a [[Right]] containing a new [[SocketIOAkkaHttp]] instance, or a [[Left]] of [[String]] containing an error message
    */
  def apply()(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer): Either[String, SocketIOAkkaHttp] = {
    apply(SocketIOAkkaHttpSettings())
  }
}

class SocketIOAkkaHttp private (settings: SocketIOAkkaHttpSettings)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) {
  private val logger = Logging.getLogger(system, getClass)

  private val transportProvider = new AkkaHttpTransportProvider(settings)
  transportProvider.init()
  SocketIOManager.getInstance.setTransportProvider(transportProvider)
  of(SocketIOProtocol.DEFAULT_NAMESPACE)

  /**
    * Returns or creates a namespace with the specified name.
    *
    * @param namespace the namespace name
    * @return a [[Namespace]]
    */
  def of(namespace: String): Namespace = {
    Option(SocketIOManager.getInstance.getNamespace(namespace))
      .getOrElse(SocketIOManager.getInstance.createNamespace(namespace))
  }

  /**
    * Shuts down this [[SocketIOAkkaHttp]] instance.
    */
  def destroy(): Unit = {
    SocketIOManager.getInstance.setTransportProvider(null)
    transportProvider.destroy()
  }

  /**
    * The Akka-HTTP [[Route]] that should be added to the appropriate place in your route structure.
    *
    * For example, you might add to your route structure as so:
    * {{{
    *   pathPrefix("socket.io") {
    *       socketIoAkkaHttp.route
    *   }
    * }}}
    *
    * Note that you should use [[pathPrefix]], as often clients will send trailing slashes, and will
    * also send further path components to access non-default namespaces.
    */
  val route: Route = {
    (get & pathSuffix("socket.io.js")) {
      complete(HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(
          MediaTypes.`application/javascript`.withCharset(HttpCharsets.`UTF-8`),
          StreamConverters.fromInputStream(() => getClass.getClassLoader.getResourceAsStream("com/codeminders/socketio/socket.io.js"))
        )
      ))
    } ~
    (get | post | options) {
      extractRequest { request =>
        extractUpgradeToWebSocket { upgrade =>
          complete(handle(request, Some(upgrade)))
        } ~
        complete(handle(request, maybeUpgrade = None))
      }
    }
  }

  private def handle(request: HttpRequest, maybeUpgrade: Option[UpgradeToWebSocket]): HttpResponse = {
    val requestWrapper = new AkkaHttpRequestWrapper(request)
    val responseWrapper = new AkkaHttpResponseWrapper()

    try {
      val transport = SocketIOManager.getInstance
        .getTransportProvider
        .getTransport(requestWrapper)

      transport.handle(requestWrapper, responseWrapper, SocketIOManager.getInstance)

      (transport, maybeUpgrade) match {
        case (wstp: AkkaHttpWebSocketTransport, Some(upgrade)) =>
          Option(wstp.getConnection(requestWrapper, SocketIOManager.getInstance)) match {
            case Some(connection: AkkaHttpWebSocketConnection) =>
              responseWrapper.toAkkaHttpResponse.map(_.entity.discardBytes())
              val session = request.uri.query().get(EngineIOProtocol.SESSION_ID)
                .flatMap(sid => Option(SocketIOManager.getInstance.getSession(sid)))
                .getOrElse(SocketIOManager.getInstance.createSession())
              connection.setSession(session)
              upgrade.handleMessagesWithSinkSource(connection.incomingFlow, connection.outgoingFlow)

            case Some(connection) =>
              logger.info(s"Got WebSocket transport but connection is {}", connection.getClass.getName)
              responseWrapper.toAkkaHttpResponse.map(_.entity.discardBytes())
              connection.abort()
              HttpResponse(StatusCodes.BadRequest)

            case _ =>
              logger.info("Got WebSocket upgrade but no connection")
              responseWrapper.toAkkaHttpResponse.map(_.entity.discardBytes())
              HttpResponse(StatusCodes.BadRequest)
          }

        case (wstp: AkkaHttpWebSocketTransport, None) =>
          logger.info("Got WebSocket transport but no upgrade")
          responseWrapper.toAkkaHttpResponse.map(_.entity.discardBytes())
          Option(wstp.getConnection(requestWrapper, SocketIOManager.getInstance)).foreach(_.abort())
          HttpResponse(StatusCodes.BadRequest)

        case _ =>
          responseWrapper.toAkkaHttpResponse.fold({ err =>
            logger.warning("Failed to create HTTP response: {}", err)
            HttpResponse(StatusCodes.InternalServerError)
          }, identity)
      }
    } catch {
      case e: UnsupportedTransportException =>
        logger.info("Unsupported socket.io transport: {}", e.getMessage)
        HttpResponse(StatusCodes.BadRequest)
      case e: SocketIOException =>
        logger.warning("Failed to process socket.io request: {}", e.getMessage)
        HttpResponse(StatusCodes.BadRequest)
      case NonFatal(e) =>
        logger.error(e, "Unexpected exception processing socket.io request")
        HttpResponse(StatusCodes.InternalServerError)
    }
  }
}
