package org.github.bromel777.yaXMPPc.configs

import derevo.derive
import derevo.pureconfig.pureconfigReader
import tofu.Context

@derive(pureconfigReader)
final case class XMPPServerSettings(host: String, port: Int)

object XMPPServerSettings extends Context.Companion[Option[XMPPServerSettings]]
