package org.github.bromel777.yaXMPPc.modules.clients

import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import fs2.Stream
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class XMPPClient[F[_]: Logging] private() extends Client[F, Stanza, Stanza] {

  override def receiverStream: Stream[F, Stanza] = ???

  override def send(out: Stanza): F[Unit] = ???

  override def start: Stream[F, Unit] =
    Stream.eval(info"Start XMPP Client")
}

object XMPPClient {

  def make[F[_]](socketGroup: SocketGroup): Client[F, Stanza, Stanza] = ???
}
