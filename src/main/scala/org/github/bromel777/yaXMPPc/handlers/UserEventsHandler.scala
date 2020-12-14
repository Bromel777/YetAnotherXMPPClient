package org.github.bromel777.yaXMPPc.handlers

import org.github.bromel777.yaXMPPc.domain.userEvents.UserEvent

final class UserEventsHandler[F[_]] private () extends Handler[F, UserEvent] {

  override def handle[E1 <: UserEvent](event: E1): F[Unit] = ???
}

object UserEventsHandler {

  def make[F[_]]: Handler[F, UserEvent] = ???
}
