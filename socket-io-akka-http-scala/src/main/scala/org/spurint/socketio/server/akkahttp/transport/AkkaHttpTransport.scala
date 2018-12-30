package org.spurint.socketio.server.akkahttp.transport

import com.codeminders.socketio.server.Config
import com.codeminders.socketio.server.transport.AbstractTransport
import org.spurint.socketio.server.akkahttp.{SocketIOAkkaHttpSettings, TypesafeBasedConfig}

private[akkahttp] abstract class AkkaHttpTransport(settings: SocketIOAkkaHttpSettings) extends AbstractTransport {
  override protected val getConfig: Config = new TypesafeBasedConfig(getType.name, settings)
}
