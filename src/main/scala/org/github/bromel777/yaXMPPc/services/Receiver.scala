package org.github.bromel777.yaXMPPc.services

/**
 * Receive all incoming messages and all their derivatives by appropriate handler
 * Derivation example:
 * Array[Byte] ~> Stanza ~> UserEvent
 * @tparam F
 */
trait Receiver[F[_], In, Out] {

  def handleMsg(input: In): F[Out]
}

object Receiver {

  def make[F[_], In, Out]: Receiver[F, In, Out] = ???
}
