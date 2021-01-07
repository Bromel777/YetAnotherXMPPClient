package org.github.bromel777.yaXMPPc

import org.github.bromel777.yaXMPPc.configs.{CASettings, XMPPSettings}
import tofu.WithContext

package object context {

  type HasXMPPConfig[F[_]] = WithContext[F, XMPPSettings]
  type HasCAConfig[F[_]] = WithContext[F, CASettings]
}
