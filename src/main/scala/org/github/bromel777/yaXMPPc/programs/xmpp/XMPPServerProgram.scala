package org.github.bromel777.yaXMPPc.programs.xmpp

import cats.Functor
import cats.effect.{Concurrent, ContextShift, Sync}
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPServerSettings
import org.github.bromel777.yaXMPPc.modules.servers.TCPServer
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class XMPPServerProgram[F[_]: Concurrent: ContextShift: Logging] private (settings: XMPPServerSettings, tcpServer: TCPServer[F]) extends Program[F] {

  override def run(commandsQueue: Queue[F, String]): Stream[F, Unit] =
    Stream.eval(info"Xmpp server started!") >> tcpServer.receiverStream
      .evalMap(stanza => info"Receive next stanza: ${stanza.toString}")

  override def executeCommand(commandsQueue: Queue[F, String]): Stream[F, Unit] = ???
}

object XMPPServerProgram {

  def make[F[_]: Concurrent: ContextShift](settings: XMPPServerSettings, socketGroup: SocketGroup)(implicit logs: Logs[F, F]): F[Program[F]] =
    logs.forService[XMPPServerProgram[F]].flatMap( implicit logging =>
      TCPServer.make[F](settings, socketGroup).map { server =>
        new XMPPServerProgram(settings, server)
      }
    )
}
