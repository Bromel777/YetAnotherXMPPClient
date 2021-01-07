package org.github.bromel777.yaXMPPc.settings

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class XMPPSettings(host: String, port: Int)
