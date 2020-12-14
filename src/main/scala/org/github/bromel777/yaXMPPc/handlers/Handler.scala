package org.github.bromel777.yaXMPPc.handlers

trait Handler[F[_], E] {

  def handle[E1 <: E](event: E1): F[Unit]
}
