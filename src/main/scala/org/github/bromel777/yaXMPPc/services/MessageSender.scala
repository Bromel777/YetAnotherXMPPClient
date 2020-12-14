package org.github.bromel777.yaXMPPc.services

import org.github.bromel777.yaXMPPc.domain.stanza.Stanza

trait MessageSender[F[_]] {

  def send[T <: Stanza](msg: T): F[Unit]
}

object MessageSender {

  def make[F[_]]: MessageSender[F] = ???

  private final class Live[F[_]] extends MessageSender[F] {
    override def send[T](msg: T): F[Unit] = ???
  }
}
