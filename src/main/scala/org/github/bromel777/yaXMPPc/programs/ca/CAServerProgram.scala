package org.github.bromel777.yaXMPPc.programs.ca

import cats.Functor
import cats.effect.Sync
import fs2.Stream
import fs2.concurrent.Queue
import org.github.bromel777.yaXMPPc.configs.CASettings
import org.github.bromel777.yaXMPPc.programs.Program
import org.github.bromel777.yaXMPPc.programs.cli.Command
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class CAServerProgram[F[_]: Logging] private(settings: CASettings) extends Program[F] {

  override def run(commandsQueue: Queue[F, Command]): Stream[F, Unit] = Stream.eval(info"CA server started!")

  override def executeCommand(commandsQueue: Queue[F, Command]): Stream[F, Unit] = ???
}

object CAServerProgram {

  def make[F[_]: Sync](settings: CASettings)(implicit logs: Logs[F, F]): F[Program[F]] =
    logs.forService[CAServerProgram[F]].map(implicit logging => new CAServerProgram(settings))
}
