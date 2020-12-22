package org.github.bromel777.yaXMPPc.modules

import fs2.Stream

/**
 * Listen cli
 * @tparam F
 */

trait CliListener[F[_]] {

  def cliListenerStream: Stream[F, Unit]
}

object CliListener {

  def make[F]: CliListener[F] = ???
}
