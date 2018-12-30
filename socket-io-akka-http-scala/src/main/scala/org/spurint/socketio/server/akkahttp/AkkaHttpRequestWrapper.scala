package org.spurint.socketio.server.akkahttp

import akka.http.scaladsl.model.{HttpRequest => AkkaHttpRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, StreamConverters}
import com.codeminders.socketio.server.HttpRequest
import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.util.Locale

private[akkahttp] class AkkaHttpRequestWrapper(request: AkkaHttpRequest)(implicit materializer: Materializer) extends HttpRequest {
  private lazy val query = request.uri.query()
  private lazy val entityInputStream = request.entity.dataBytes.toMat(StreamConverters.asInputStream())(Keep.right).run
  private lazy val entityReader = new BufferedReader(new InputStreamReader(entityInputStream))

  override def getMethod: String = request.method.value

  override def getHeader(name: String): String =
    request.headers.find(_.lowercaseName == name.toLowerCase(Locale.US)).map(_.value).orNull

  override def getContentType: String = request.entity.contentType.value

  override def getParameter(name: String): String = query.get(name).orNull

  override def getInputStream: InputStream = entityInputStream

  override def getReader: BufferedReader = entityReader
}
