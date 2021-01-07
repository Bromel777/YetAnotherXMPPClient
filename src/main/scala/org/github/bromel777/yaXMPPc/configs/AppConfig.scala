package org.github.bromel777.yaXMPPc.configs

import cats.effect.Sync
import derevo.derive
import derevo.pureconfig.pureconfigReader
import pureconfig.ConfigSource
import pureconfig.module.catseffect._

@derive(pureconfigReader)
final case class AppConfig(
  commonSettings: CommonSettings,
  XMPPSettings: Option[XMPPSettings],
  caSettings: Option[CASettings]
)

object AppConfig {

  def load[F[_]: Sync](pathOpt: Option[String]): F[AppConfig] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, AppConfig]
}
