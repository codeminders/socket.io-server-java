package org.spurint.socketio.server.akkahttp.util

import java.util.concurrent.ArrayBlockingQueue
import scala.concurrent.{Future, Promise}

private[akkahttp] object MessageQueue {
  case object QueueClosed extends RuntimeException
}

private[akkahttp] class MessageQueue[T](depth: Int) extends AutoCloseable {
  private val futureQueue = new ArrayBlockingQueue[Future[T]](depth)
  @volatile private var pendingPromise: Option[Promise[T]] = None
  @volatile private var closed: Boolean = false

  private val _closed = Promise[Unit]()
  val closedFuture: Future[Unit] = _closed.future

  def offer(elem: T): Boolean = {
    synchronized {
      if (closed) {
        false
      } else {
        pendingPromise.map { promise =>
          promise.success(elem)
          pendingPromise = None
          true
        }.getOrElse(
          futureQueue.offer(Future.successful(elem))
        )
      }
    }
  }

  def error(ex: Throwable): Boolean = {
    synchronized {
      if (closed) {
        false
      } else {
        pendingPromise.map { promise =>
          promise.failure(ex)
          pendingPromise = None
          close()
          true
        }.getOrElse {
          futureQueue.offer(Future.failed(ex))
          close()
          true
        }
      }
    }
  }

  def take(): Future[T] = {
    synchronized {
      Option(futureQueue.poll()).getOrElse {
        if (closed) {
          Future.failed(MessageQueue.QueueClosed)
        } else {
          assert(pendingPromise.isEmpty)
          val promise = Promise[T]()
          pendingPromise = Some(promise)
          promise.future
        }
      }
    }
  }

  override def close(): Unit = {
    synchronized {
      closed = true
      pendingPromise.foreach { promise =>
        promise.failure(MessageQueue.QueueClosed)
        pendingPromise = None
      }
    }
    _closed.trySuccess(())
  }
}
