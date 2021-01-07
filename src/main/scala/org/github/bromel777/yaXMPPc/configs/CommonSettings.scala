package org.github.bromel777.yaXMPPc.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
case class CommonSettings(
  programType: String
)

object CommonSettings extends Context.Companion[CommonSettings]
