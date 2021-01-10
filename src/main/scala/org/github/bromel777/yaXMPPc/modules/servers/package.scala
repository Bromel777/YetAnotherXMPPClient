package org.github.bromel777.yaXMPPc.modules

import java.util.UUID

package object servers {

  sealed trait TCPEvent
  object TCPEvent {
    case class ClientDisconnected(UUID: UUID) extends TCPEvent
    case class ClientConnected(UUID: UUID) extends TCPEvent
  }
}
