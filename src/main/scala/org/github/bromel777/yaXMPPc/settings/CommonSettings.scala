package org.github.bromel777.yaXMPPc.settings

import derevo.derive
import org.manatki.derevo.pureconfigDerivation.pureconfigReader

@derive(pureconfigReader)
case class CommonSettings(
  programType: String
)
