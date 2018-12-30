package org.spurint.socketio.server.akkahttp

import akka.http.scaladsl.model
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl.StreamConverters
import com.codeminders.socketio.server.HttpResponse
import java.io.{OutputStream, PipedInputStream, PipedOutputStream}
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._

private[akkahttp] class AkkaHttpResponseWrapper extends HttpResponse with AutoCloseable {
  @volatile private var statusCode: StatusCode = StatusCodes.OK
  private val headers = new ConcurrentHashMap[String, String]
  @volatile private var contentType: Option[String] = None

  private lazy val inputStream = new PipedInputStream()
  private lazy val outputStream = new PipedOutputStream(inputStream)
  private lazy val entitySource = StreamConverters.fromInputStream(() => inputStream)

  override def setHeader(name: String, value: String): Unit = headers.put(name, value)

  override def setContentType(contentType: String): Unit = this.contentType = Option(contentType)

  override def getOutputStream: OutputStream = outputStream

  override def sendError(statusCode: Int, message: String): Unit = {
    this.statusCode = parseStatusCode(statusCode)
    outputStream.write(message.getBytes(StandardCharsets.UTF_8))
    close()
  }

  override def sendError(statusCode: Int): Unit = {
    this.statusCode = parseStatusCode(statusCode)
    close()
  }

  override def flushBuffer(): Unit = outputStream.flush()

  override def close(): Unit = {
    flushBuffer()
    outputStream.close()
  }

  lazy val toAkkaHttpResponse: Either[String, model.HttpResponse] = {
    close()
    this.contentType.map(s => ContentType.parse(s)).getOrElse(Right(ContentTypes.NoContentType)).map { contentType =>
      model.HttpResponse(
        status = statusCode,
        headers = headers.asScala.map { case (name, value) => RawHeader(name, value) }.to[scala.collection.immutable.Seq],
        entity = HttpEntity(contentType, entitySource)
      )
    }.swap.map { errors => errors.map(_.summary).mkString("; ") }.swap
  }

  private def parseStatusCode(statusCode: Int): StatusCode = {
    StatusCodes.getForKey(statusCode).getOrElse(StatusCodes.custom(statusCode, "", ""))
  }
}
