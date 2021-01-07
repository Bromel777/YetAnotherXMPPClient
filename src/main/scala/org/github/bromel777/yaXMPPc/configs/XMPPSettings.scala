package org.github.bromel777.yaXMPPc.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
final case class XMPPSettings(host: String, port: Int)

object XMPPSettings extends Context.Companion[Option[XMPPSettings]]
