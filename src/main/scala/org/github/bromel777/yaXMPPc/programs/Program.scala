package org.github.bromel777.yaXMPPc.programs

import derevo.derive
import fs2.Stream
import tofu.fs2Instances._
import tofu.higherKind.derived.embed

@derive(embed)
trait Program[F[_]] {

  def run: Stream[F, Unit]
}
