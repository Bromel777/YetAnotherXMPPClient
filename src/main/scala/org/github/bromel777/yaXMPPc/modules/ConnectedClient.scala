package org.github.bromel777.yaXMPPc.modules

import java.util.UUID

import cats.effect.{Concurrent, Sync}
import fs2.io.tcp.Socket
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import tofu.logging.Logging
import tofu.syntax.monadic._

final class ConnectedClient[F[_]] private (
  id: UUID,
  messageSocket: MessageSocket[F, Stanza, Stanza]
)

private object ConnectedClient {

  def apply[F[_]: Concurrent: Logging](socket: Socket[F]): F[ConnectedClient[F]] =
    for {
      id <- Sync[F].delay(UUID.randomUUID)
      messageSocket <- MessageSocket(
                         socket,
                         Stanza.stanzaCodec,
                         Stanza.stanzaCodec,
                         1024
                       )
    } yield new ConnectedClient(id, messageSocket)
}
