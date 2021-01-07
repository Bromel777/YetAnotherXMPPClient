package org.github.bromel777.yaXMPPc.programs.xmpp

import cats.effect.Sync
import fs2.Stream
import org.github.bromel777.yaXMPPc.configs.XMPPSettings
import org.github.bromel777.yaXMPPc.context.HasXMPPConfig
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.syntax.context._
import tofu.syntax.embed._
import tofu.syntax.monadic._

final class Client[F[_]] private (settings: XMPPSettings) extends Program[F] {

  override def run: Stream[F, Unit] = ???
}

object Client {

  def make[F[_]: HasXMPPConfig: Sync]: Program[F] =
    context.map(new Client(_): Program[F]).embed
}
