package org.github.bromel777.yaXMPPc.modules.servers

import fs2.Stream
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza

/**
 * Provide connection to xmpp server, handle all incoming and outgoing stanzas
 * @return
 */
final class XMPPServer[F[_]]() extends Server[F, Stanza, Stanza] {

  override def receiverStream: Stream[F, Stanza] = ???

  override def send(out: Stanza): F[Unit] = ???
}

object XMPPServer {

  def make[F[_]]: XMPPServer[F] = ???
}
