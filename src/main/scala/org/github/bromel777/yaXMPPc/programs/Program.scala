package org.github.bromel777.yaXMPPc.programs

import derevo.derive
import fs2.Stream
import fs2.concurrent.Queue
import org.github.bromel777.yaXMPPc.programs.cli.Command
import tofu.fs2Instances._
import tofu.higherKind.derived.embed

@derive(embed)
trait Program[F[_]] {
  def run(commandsQueue: Queue[F, Command]): Stream[F, Unit]
  def executeCommand(commandsQueue: Queue[F, Command]): Stream[F, Unit]
}
