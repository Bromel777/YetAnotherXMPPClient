package org.github.bromel777.yaXMPPc.modules.clients

import fs2.Stream

trait Client[F[_], In, Out] {
  def start: Stream[F, Unit]
  def receiverStream: Stream[F, In]
  def send(out: Out): F[Unit]
}
