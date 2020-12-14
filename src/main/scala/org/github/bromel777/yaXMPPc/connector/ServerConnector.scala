package org.github.bromel777.yaXMPPc.connector

import fs2.Stream

trait ServerConnector[F[_]] {

  /**
   * Provide connection to xmpp server
   * @return
   */
  def iterationStream: Stream[F, Unit]
}

object ServerConnector {

  def make[F[_]]: ServerConnector[F] = ???

  final class Live[F[_]] extends ServerConnector[F] {
    override def iterationStream: Stream[F, Unit] = ???
  }
}
