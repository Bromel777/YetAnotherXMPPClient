package org.github.bromel777.yaXMPPc

import cats.Applicative
import cats.effect.{ExitCode, Resource, Sync}
import fs2.Stream
import monix.eval.{Task, TaskApp}
import org.github.bromel777.yaXMPPc.args.Args
import org.github.bromel777.yaXMPPc.configs.AppConfig
import org.github.bromel777.yaXMPPc.context.{AppContext, HasAppContext}
import org.github.bromel777.yaXMPPc.errors.Err
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.Raise.ContravariantRaise
import tofu.WithRun
import tofu.env.Env
import tofu.logging.Logs
import tofu.syntax.monadic._
import tofu.syntax.handle._

object Application extends TaskApp {

  type InitF[+A] = Task[A]
  type AppF[+A]  = Env[AppContext, A]

  private val wr: WithRun[AppF, InitF, AppContext] = implicitly

  implicit val logsFF: Logs[AppF, AppF] = Logs.sync[AppF, AppF]

  override def run(args: List[String]): InitF[ExitCode] =
    (Args.read[InitF](args) >>= (argsList =>
        AppConfig.load[InitF](argsList.configPathOpt) >>= { cfg =>
          mkResources[InitF, AppF](cfg).use { programs =>
            Stream
              .emits(
                programs.map(
                  _.run.translate(wr.runContextK(AppContext(cfg.commonSettings, cfg.XMPPServerSettings, cfg.caSettings)))
                )
              )
              .parJoinUnbounded
              .compile
              .drain as ExitCode.Success
          }
        }
      )).handleToWith[InitF, Err](e => Sync[InitF].delay(println(s"Error during program work pipeline: ${e.msg}")) as ExitCode.Error)

  def mkResources[I[_]: Applicative, F[_]: HasAppContext: Sync: ContravariantRaise[*[_], Error]](
    cfg: AppConfig
  )(implicit logs: Logs[F, F]): Resource[I, List[Program[F]]] =
    Resource.pure[I, List[Program[F]]](List(programs.getProgram[F](cfg.commonSettings.programType)))
}
