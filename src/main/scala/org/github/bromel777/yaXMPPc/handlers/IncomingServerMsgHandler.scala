package org.github.bromel777.yaXMPPc.handlers

final class IncomingServerMsgHandler[F[_]] private () extends Handler[F, Array[Byte]] {

  override def handle[E1 <: Array[Byte]](event: E1): F[Unit] = ???
}

object IncomingServerMsgHandler {

  def make[F[_]]: Handler[F, Array[Byte]] = ???
}
