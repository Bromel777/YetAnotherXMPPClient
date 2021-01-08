package org.github.bromel777.yaXMPPc.programs.cli

import fs2.Stream
import fs2.concurrent.Queue
import tofu.common.Console

import scala.tools.nsc.ScalaDoc.Command

final class CLIProgram[F[_]: Console](commandsQueue: Queue[F, String]) {

  def run: Stream[F, Unit] = Stream.repeatEval(Console[F].readStrLn).evalMap(commandsQueue.enqueue1)
}

object CLIProgram {

  def make[F[_]: Console](commandsQueue: Queue[F, String]): Stream[F, Unit] =
    new CLIProgram(commandsQueue).run
}
