package org.github.bromel777.yaXMPPc.configs

import cats.effect.Sync
import derevo.derive
import derevo.pureconfig.pureconfigReader
import pureconfig.ConfigSource
import pureconfig.module.catseffect._
import tofu.Context

@derive(pureconfigReader)
final case class AppConfig(
                            commonSettings: CommonSettings,
                            XMPPServerSettings: Option[XMPPServerSettings],
                            XMPPClientSettings: Option[XMPPServerSettings],
                            caSettings: Option[CASettings]
)

object AppConfig extends Context.Companion[AppConfig] {

  def load[F[_]: Sync](pathOpt: Option[String]): F[AppConfig] =
    pathOpt
      .map(ConfigSource.file)
      .getOrElse(ConfigSource.default)
      .loadF[F, AppConfig]
}
