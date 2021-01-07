package org.github.bromel777.yaXMPPc

import cats.Applicative
import cats.effect.{Blocker, Concurrent, ContextShift, ExitCode, Resource, Sync, Timer}
import fs2.Stream
import fs2.io.tcp.SocketGroup
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
import tofu.syntax.handle._
import tofu.syntax.monadic._

import scala.concurrent.ExecutionContext

object Application extends TaskApp {

  type InitF[+A] = Task[A]
  type AppF[+A]  = Env[AppContext, A]

  private val wr: WithRun[AppF, InitF, AppContext] = implicitly

  implicit val logsFF: Logs[AppF, AppF] = Logs.sync[AppF, AppF]

  implicit val blocker: Blocker = Blocker.liftExecutionContext(ExecutionContext.global)

  override def run(args: List[String]): InitF[ExitCode] =
    (Args.read[InitF](args) >>= (argsList =>
        AppConfig.load[InitF](argsList.configPathOpt) >>= { cfg =>
          mkResources[InitF, AppF](cfg).use { programs =>
            Stream
              .emits(
                programs.map(
                  _.run.translate(
                    wr.runContextK(AppContext(cfg.commonSettings, cfg.XMPPServerSettings, cfg.XMPPClientSettings, cfg.caSettings))
                  )
                )
              )
              .parJoinUnbounded
              .compile
              .drain as ExitCode.Success
          }
        }
      )).handleToWith[InitF, Err](e =>
      Sync[InitF].delay(println(s"Error during program work pipeline: ${e.msg}")) as ExitCode.Error
    )

  def mkResources[I[_]: Sync, F[_]: Concurrent: HasAppContext: ContextShift: ContravariantRaise[*[_], Error]: Timer](
    cfg: AppConfig
  )(implicit logs: Logs[F, F], blocker: Blocker, withRun: WithRun[F, I, AppContext]): Resource[I, List[Program[F]]] =
    SocketGroup[F](blocker).map { socketGroup =>
      List(programs.getProgram[F](cfg.commonSettings.programType, socketGroup))
    }.mapK(withRun.runContextK(AppContext(cfg.commonSettings, cfg.XMPPServerSettings, cfg.XMPPClientSettings, cfg.caSettings)))
}
