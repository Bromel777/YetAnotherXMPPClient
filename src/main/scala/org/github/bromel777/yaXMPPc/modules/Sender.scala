package org.github.bromel777.yaXMPPc.modules

import fs2.Stream

/**
 * Module responsible for sending external messages to another server.
 * @tparam F
 */
trait Sender[F[_]] {

  def senderStream: Stream[F, Unit]
}

object Sender {

  def make[F]: Sender[F] = ???
}
