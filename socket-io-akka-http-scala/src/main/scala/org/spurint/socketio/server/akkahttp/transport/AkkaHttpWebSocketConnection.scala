package org.spurint.socketio.server.akkahttp.transport

import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.util.FastFuture
import akka.stream._
import akka.stream.scaladsl.{Keep, StreamConverters}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.codeminders.socketio.common.SocketIOException
import com.codeminders.socketio.server.Transport
import com.codeminders.socketio.server.transport.websocket.AbstractWebsocketTransportConnection
import org.spurint.socketio.server.akkahttp.util.MessageQueue
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

private[akkahttp] class AkkaHttpWebSocketConnection(transport: Transport)
                                                   (implicit ec: ExecutionContext,
                                                    mat: Materializer)
  extends AbstractWebsocketTransportConnection(transport)
{
  private val outgoingQueue = new MessageQueue[Message](128)

  override def abort(): Unit = {
    super.abort()
    outgoingQueue.close()
  }

  override protected def sendString(data: String): Unit = outgoingQueue.offer(TextMessage(data))

  override protected def sendBinary(data: Array[Byte]): Unit = outgoingQueue.offer(BinaryMessage(ByteString(data)))

  lazy val outgoingFlow: Graph[SourceShape[Message], Any] = new GraphStage[SourceShape[Message]] {
    val out = Outlet[Message]("Message.out")

    override def shape: SourceShape[Message] = SourceShape.of(out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      override def preStart(): Unit = sendHandshake()

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          val messageCallback = getAsyncCallback[Message](push(out, _))
          val closeCallback = getAsyncCallback[Unit](_ => completeStage())
          val errorCallback = getAsyncCallback[Throwable] { t =>
            failStage(t)
            abort()
          }

          outgoingQueue.take().onComplete {
            case Success(message) => messageCallback.invoke(message)
            case Failure(MessageQueue.QueueClosed) => closeCallback.invoke(())
            case Failure(t) => errorCallback.invoke(t)
          }
        }

        override def onDownstreamFinish(): Unit = {
          super.onDownstreamFinish()
          outgoingQueue.close()
        }
      })
    }
  }

  lazy val incomingFlow: Graph[SinkShape[Message], Any] = new GraphStage[SinkShape[Message]] {
    val in = Inlet[Message]("Message.in")

    override def shape: SinkShape[Message] = SinkShape.of(in)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      override def preStart(): Unit = pull(in)

      val closedCallback = getAsyncCallback[Unit](_ => completeStage())
      outgoingQueue.closedFuture.onComplete(_ => closedCallback.invoke(()))

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val successCallback = getAsyncCallback[Unit](_ => pull(in))
          val errorCallback = getAsyncCallback[Throwable] { t =>
            failStage(t)
            abort()
          }

          (grab(in) match {
            case tm: TextMessage =>
              tm.textStream
                .runFold(new StringBuilder)((accum, next) => accum.append(next))
                .map(_.toString)
                .map(handleTextFrame)

            case bm: BinaryMessage =>
              FastFuture(Try(
                handleBinaryFrame(bm.dataStream.toMat(StreamConverters.asInputStream())(Keep.right).run())
              ))
          }).onComplete {
            case Success(wasSuccessful) if wasSuccessful => successCallback.invoke(())
            case Success(_) => errorCallback.invoke(new SocketIOException("Failed to decode incoming message"))
            case Failure(t) => errorCallback.invoke(t)
          }
        }

        override def onUpstreamFinish(): Unit = {
          outgoingQueue.close()
          super.onUpstreamFinish()
        }

        override def onUpstreamFailure(t: Throwable): Unit = {
          super.onUpstreamFailure(t)
          abort()
        }
      })
    }
  }
}
