package org.spurint.socketio.server.akkahttp

import com.codeminders.socketio.server.Config

private[akkahttp] class TypesafeBasedConfig(namespace: String, settings: SocketIOAkkaHttpSettings) extends Config {
  override def getPingInterval(default: Long): Long = settings.pingInterval.toMillis

  override def getTimeout(default: Long): Long = settings.pingTimeout.toMillis

  override def getBufferSize: Int = Config.DEFAULT_BUFFER_SIZE

  override def getMaxIdle: Int = Config.DEFAULT_MAX_IDLE

  override def getString(key: String): String = key match {
    case "allowedOrigins" => settings.allowedOrigins.mkString(",")
    case _ => null
  }

  override def getString(key: String, default: String): String = key match {
    case "allowedOrigins" => settings.allowedOrigins.mkString(",")
    case _ => default
  }

  override def getInt(key: String, default: Int): Int = key match {
    case Config.MAX_TEXT_MESSAGE_SIZE => default
    case Config.BUFFER_SIZE => getBufferSize
    case Config.MAX_IDLE => getMaxIdle
    case _ => default
  }

  override def getLong(key: String, default: Long): Long = key match {
    case Config.PING_INTERVAL => getPingInterval(default)
    case Config.TIMEOUT => getTimeout(default)
    case Config.MAX_TEXT_MESSAGE_SIZE => default
    case Config.BUFFER_SIZE => getBufferSize
    case Config.MAX_IDLE => getMaxIdle
    case _ => default
  }

  override def getBoolean(key: String, default: Boolean): Boolean = key match {
    case "allowAllOrigins" => settings.allowAllOrigins
    case _ => default
  }

  override def getNamespace: String = namespace
}
