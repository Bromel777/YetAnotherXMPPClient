package org.github.bromel777.yaXMPPc.args

import cats.effect.Sync
import cats.syntax.option._
import scopt.OParser
import tofu.syntax.monadic._

final case class Args(configPathOpt: Option[String])

object Args {

  private val builder = OParser.builder[Args]

  private val parser = {
    import builder._
    OParser.sequence(
      opt[String]('p', "configPath")
        .action((path, args) => args.copy(configPathOpt = path.some))
        .text("Path to config file")
    )
  }

  private val defaultCfg = Args(none)

  def read[F[_]: Sync](args: List[String]): F[Args] =
    OParser.parse(parser, args, defaultCfg) match {
      case Some(config) => config.pure[F]
      case _ =>
        Sync[F].delay(println("Error occurred during parsing arguments. Start with default config!")) >> defaultCfg
          .pure[F]
    }

}
