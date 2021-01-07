package org.github.bromel777.yaXMPPc.programs.xmpp

import cats.Functor
import cats.effect.Sync
import fs2.Stream
import org.github.bromel777.yaXMPPc.configs.XMPPServerSettings
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class XMPPServerProgram[F[_]: Logging] private(settings: XMPPServerSettings) extends Program[F] {

  override def run: Stream[F, Unit] = Stream.eval(info"Xmpp server started!")
}

object XMPPServerProgram {

  def make[F[_]: Sync](settings: XMPPServerSettings)(implicit logs: Logs[F, F]): F[Program[F]] =
    logs.forService[XMPPServerProgram[F]].map(implicit logging => new XMPPServerProgram(settings))
}
