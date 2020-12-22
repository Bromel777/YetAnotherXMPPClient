package org.github.bromel777.yaXMPPc.modules

import fs2.Stream

/**
 * Receive all incoming messages and all their derivatives with appropriate handler
 * Derivation example:
 * Array[Byte] ~> Stanza ~> UserEvent
 * @tparam F
 */
trait Receiver[F[_]] {

  def receiverStream: Stream[F, Unit]
}

object Receiver {

  def make[F[_]]: Receiver[F] = ???
}
