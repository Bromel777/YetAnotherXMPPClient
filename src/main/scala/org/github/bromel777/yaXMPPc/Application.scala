package org.github.bromel777.yaXMPPc

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}
import org.github.bromel777.yaXMPPc.args.Args
import org.github.bromel777.yaXMPPc.context.AppContext
import tofu.env.Env

object Application extends TaskApp {

  type InitF[+A]   = Task[A]
  type AppF[+A]    = Env[AppContext, A]

  override def run(args: List[String]): Task[ExitCode] = ???

  def mkResources[I[_]](args: Args) = ???
}
