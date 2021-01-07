package org.github.bromel777.yaXMPPc.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader

@derive(pureconfigReader)
final case class XMPPClientSettings(serverHost: String, serverPort: Int)
