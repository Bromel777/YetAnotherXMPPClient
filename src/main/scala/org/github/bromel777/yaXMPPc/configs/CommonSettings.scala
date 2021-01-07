package org.github.bromel777.yaXMPPc.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
case class CommonSettings(
  programType: String
)
