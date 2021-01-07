package org.github.bromel777.yaXMPPc.programs.xmpp

import cats.Functor
import cats.effect.Sync
import fs2.Stream
import org.github.bromel777.yaXMPPc.configs.XMPPSettings
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class XMPPClientProgram[F[_]: Logging] private(settings: XMPPSettings) extends Program[F] {

  override def run: Stream[F, Unit] = Stream.eval(info"Xmpp client started!")
}

object XMPPClientProgram {

  def make[F[_]: Sync](settings: XMPPSettings)(implicit logs: Logs[F, F]): F[Program[F]] =
    logs.forService[XMPPClientProgram[F]].map(implicit logging => new XMPPClientProgram(settings))
}
