package org.github.bromel777.yaXMPPc.handlers

import org.github.bromel777.yaXMPPc.domain.stanza.Stanza

final class StanzaHandler[F[_]] private () extends Handler[F, Stanza]{

  override def handle[E1 <: Stanza](event: E1): F[Unit] = ???
}

object StanzaHandler {

  def make[F[_]]: Handler[F, Stanza] = ???
}
