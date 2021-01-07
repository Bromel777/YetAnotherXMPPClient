package org.github.bromel777.yaXMPPc.programs.ca

import cats.effect.Sync
import org.github.bromel777.yaXMPPc.programs.Program
import fs2.Stream
import org.github.bromel777.yaXMPPc.configs.CASettings
import org.github.bromel777.yaXMPPc.context.HasCAConfig
import tofu.syntax.embed._
import tofu.syntax.context._
import tofu.syntax.monadic._

final class Server[F[_]] private (settings: CASettings) extends Program[F] {

  override def run: Stream[F, Unit] = ???
}

object Server {

  def make[F[_]: HasCAConfig: Sync]: Program[F] =
    context.map(new Server(_): Program[F]).embed
}
