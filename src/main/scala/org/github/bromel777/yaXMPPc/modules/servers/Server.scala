package org.github.bromel777.yaXMPPc.modules.servers

import java.util.UUID

import fs2.Stream

trait Server[F[_], In, Out] {
  def receiverStream: Stream[F, In]
  def send(out: Out, user: UUID): F[Unit]
}
