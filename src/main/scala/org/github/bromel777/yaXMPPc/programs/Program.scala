package org.github.bromel777.yaXMPPc.programs

import fs2.Stream

trait Program[F[_]] {

  def run: Stream[F, Unit]
}
