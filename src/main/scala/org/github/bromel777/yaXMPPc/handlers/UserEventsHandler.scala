package org.github.bromel777.yaXMPPc.handlers

import org.github.bromel777.yaXMPPc.domain.userEvents.UserEvent

trait UserEventsHandler[F[_]] {

  def process[E <: UserEvent](event: UserEvent): F[Unit]
}

object UserEventsHandler {

  def make[F[_]]: UserEventsHandler[F] = ???

  final private class Live[F[_]] extends UserEventsHandler[F] {

    override def process[E <: UserEvent](event: UserEvent): F[Unit] = ???
  }
}
