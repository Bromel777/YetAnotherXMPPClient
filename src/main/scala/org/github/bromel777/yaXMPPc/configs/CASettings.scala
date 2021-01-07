package org.github.bromel777.yaXMPPc.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
final case class CASettings()

object CASettings extends Context.Companion[Option[CASettings]]
