package org.github.bromel777.yaXMPPc

import cats.Applicative
import cats.effect.{ExitCode, Resource}
import fs2.Stream
import monix.eval.{Task, TaskApp}
import org.github.bromel777.yaXMPPc.args.Args
import org.github.bromel777.yaXMPPc.configs.AppConfig
import org.github.bromel777.yaXMPPc.context.AppContext
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.WithRun
import tofu.env.Env
import tofu.syntax.monadic._

object Application extends TaskApp {

  type InitF[+A] = Task[A]
  type AppF[+A]  = Env[AppContext, A]

  private val wr: WithRun[AppF, InitF, AppContext] = implicitly

  override def run(args: List[String]): InitF[ExitCode] =
    Args.read(args) >>= (argsList =>
        AppConfig.load[InitF](argsList.configPathOpt) >>= { cfg =>
          mkResources[InitF, AppF](argsList).use { programs =>
            Stream
              .emits(
                programs.map(_.run.translate(wr.runContextK(AppContext(cfg))))
              )
              .parJoinUnbounded
              .compile
              .drain as ExitCode.Success
          }
        }
      )

  def mkResources[I[_]: Applicative, F[_]: Applicative](args: Args): Resource[I, List[Program[F]]] =
    Resource.pure[I, List[Program[F]]](List.empty[Program[F]])
}
