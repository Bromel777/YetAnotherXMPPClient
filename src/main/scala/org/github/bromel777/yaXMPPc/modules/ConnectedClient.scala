package org.github.bromel777.yaXMPPc.modules

import java.util.UUID

import cats.effect.{Concurrent, Sync}
import fs2.io.tcp.Socket
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.Stanza
import tofu.logging.Logging
import tofu.syntax.monadic._

final case class ConnectedClient[F[_]](
  id: UUID,
  messageSocket: MessageSocket[F, String, String]
)

private object ConnectedClient {

  import scodec.codecs.utf8

  def apply[F[_]: Concurrent: Logging](socket: Socket[F]): F[ConnectedClient[F]] =
    for {
      id <- Sync[F].delay(UUID.randomUUID)
      messageSocket <- MessageSocket(
                         socket,
                         utf8,
                         utf8,
                         1024
                       )
    } yield new ConnectedClient(id, messageSocket)
}
