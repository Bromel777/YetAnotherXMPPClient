package org.github.bromel777.yaXMPPc.programs.xmpp

import cats.effect.{Concurrent, ContextShift, Timer}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPClientSettings
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import org.github.bromel777.yaXMPPc.modules.clients.{Client, TCPClient}
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

import scala.concurrent.duration._

final class XMPPClientProgram[F[_]: Logging: Timer] private (
  settings: XMPPClientSettings,
  tcpClient: Client[F, Stanza, Stanza]
) extends Program[F] {

  override def run: Stream[F, Unit] = Stream.eval(info"Xmpp client started!") >> mainPipe.evalMap { stanza =>
    info"Receive: ${stanza.toString}"
  }

  private def mainPipe: Stream[F, Stanza] =
    tcpClient.read
      .onComplete(
        Stream.eval(warn"Connection to XMPP server was closed! Retry after 3 Seconds") >> Stream.sleep(
          3 seconds
        ) >> mainPipe
      )
}

object XMPPClientProgram {

  def make[F[_]: Concurrent: ContextShift: Timer](settings: XMPPClientSettings, socketGroup: SocketGroup)(implicit
    logs: Logs[F, F]
  ): F[Program[F]] =
    logs.forService[XMPPClientProgram[F]].flatMap { implicit logging =>
      TCPClient.make[F](settings.serverHost, settings.serverPort, socketGroup).map { client =>
        new XMPPClientProgram(settings, client)
      }
    }
}
