package org.spurint.socketio.server.akkahttp

import com.typesafe.config.{Config, ConfigFactory}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

object SocketIOAkkaHttpSettings {
  private val REFERENCE_CONFIG_PATH = "com.codeminders.socket-io.server.akka-http"

  /**
    * Constructs a new [[SocketIOAkkaHttpSettings]] instance based on default settings
    * @return a new [[SocketIOAkkaHttpSettings]]
    */
  def apply(): SocketIOAkkaHttpSettings = apply(ConfigFactory.load().getConfig(REFERENCE_CONFIG_PATH))

  /**
    * Constructs a new [[SocketIOAkkaHttpSettings]] instance based on values provided in the Typesafe Config
    *
    * @param config a Typesafe Config object
    * @return a new [[SocketIOAkkaHttpSettings]]
    */
  def apply(config: Config): SocketIOAkkaHttpSettings = {
    val configWithFallbacks = config.withFallback(ConfigFactory.defaultReference().getConfig(REFERENCE_CONFIG_PATH))
    SocketIOAkkaHttpSettings(
      allowAllOrigins = configWithFallbacks.getBoolean("allow-all-origins"),
      allowedOrigins = configWithFallbacks.getStringList("allowed-origins").asScala,
      pingInterval = FiniteDuration(configWithFallbacks.getDuration("ping-interval", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
      pingTimeout = FiniteDuration(configWithFallbacks.getDuration("ping-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
    )
  }
}

/**
  * Object describing settings for the Akka-HTTP Socket.IO adapter.
  *
  * @param allowAllOrigins whether or not to allow all origins via CORS
  * @param allowedOrigins a list of CORS allowed origins (only used if [[allowAllOrigins]] is false)
  * @param pingInterval interval at which to expect application-level pings
  * @param pingTimeout time after which a connection will be considered dead if a ping has not been received
  */
case class SocketIOAkkaHttpSettings(allowAllOrigins: Boolean,
                                    allowedOrigins: Seq[String],
                                    pingInterval: FiniteDuration,
                                    pingTimeout: FiniteDuration)
