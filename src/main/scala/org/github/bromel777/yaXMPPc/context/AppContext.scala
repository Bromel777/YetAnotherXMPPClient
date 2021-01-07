package org.github.bromel777.yaXMPPc.context

import org.github.bromel777.yaXMPPc.configs.{CASettings, CommonSettings, XMPPClientSettings, XMPPServerSettings}
import tofu.Context
import tofu.logging.Loggable
import tofu.optics.macros.promote

final case class AppContext(
  @promote commonSettings: CommonSettings,
  XMPPServerSettings: Option[XMPPServerSettings],
  XMPPClientSettings: Option[XMPPClientSettings],
  caSettings: Option[CASettings]
)

object AppContext extends Context.Companion[AppContext] {
  implicit val loggable: Loggable[AppContext] = Loggable.empty[AppContext]
}
