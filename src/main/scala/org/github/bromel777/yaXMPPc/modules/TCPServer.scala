package org.github.bromel777.yaXMPPc.modules

import fs2.Stream

trait TCPServer[F[_]] {

  def serverStream: Stream[F, Unit]
}

object TCPServer {

  def make[F]: TCPServer[F] = ???
}
