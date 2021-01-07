package org.github.bromel777.yaXMPPc.servers

import fs2.Stream

/**
  * Server, which establish TCP
 */

final class TCPServer[F[_]]() extends Server[F, Array[Byte], Array[Byte]] {

  override def receiverStream: Stream[F, Array[Byte]] = ???

  override def send(out: Array[Byte]): F[Unit] = ???
}

object TCPServer {

  def make[F[_]]: TCPServer[F] = ???
}
