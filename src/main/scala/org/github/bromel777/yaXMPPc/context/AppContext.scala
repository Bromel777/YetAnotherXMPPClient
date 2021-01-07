package org.github.bromel777.yaXMPPc.context

import org.github.bromel777.yaXMPPc.configs.{CASettings, CommonSettings, XMPPSettings}
import tofu.Context
import tofu.optics.macros.promote

final case class AppContext(
  @promote commonSettings: CommonSettings,
  XMPPSettings: Option[XMPPSettings],
  caSettings: Option[CASettings]
)

object AppContext extends Context.Companion[AppContext]
