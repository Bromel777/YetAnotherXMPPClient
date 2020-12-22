package org.github.bromel777.yaXMPPc.services

import org.github.bromel777.yaXMPPc.domain.stanza.Stanza

trait StanzaSender[F[_]] {

  def send[T <: Stanza](msg: T): F[Unit]
}

object StanzaSender {

  def make[F[_]]: StanzaSender[F] = ???

  private final class Live[F[_]] extends StanzaSender[F] {
    override def send[T](msg: T): F[Unit] = ???
  }
}
