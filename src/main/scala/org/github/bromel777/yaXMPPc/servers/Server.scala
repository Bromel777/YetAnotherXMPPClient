package org.github.bromel777.yaXMPPc.servers

import fs2.Stream

trait Server[F[_], In, Out] {
  def receiverStream: Stream[F, In]
  def send(out: Out): F[Unit]
}
