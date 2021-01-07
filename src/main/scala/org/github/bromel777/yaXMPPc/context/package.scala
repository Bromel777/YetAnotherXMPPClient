package org.github.bromel777.yaXMPPc

import tofu.HasContext

package object context {

  type HasAppContext[F[_]] = HasContext[F, AppContext]
}
