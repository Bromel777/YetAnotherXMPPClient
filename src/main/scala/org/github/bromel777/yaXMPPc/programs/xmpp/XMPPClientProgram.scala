package org.github.bromel777.yaXMPPc.programs.xmpp

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPClientSettings
import org.github.bromel777.yaXMPPc.domain.stanza.{Message, Receiver, Sender, Stanza}
import org.github.bromel777.yaXMPPc.modules.clients.{Client, TCPClient}
import org.github.bromel777.yaXMPPc.programs.Program
import tofu.common.Console
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

import scala.concurrent.duration._

final class XMPPClientProgram[F[_]: Concurrent: Console: Logging: Timer] private (
  settings: XMPPClientSettings,
  tcpClient: Client[F, Stanza, Stanza]
) extends Program[F] {

  override def run(commandsQueue: Queue[F, String]): Stream[F, Unit] =
    Stream.eval(info"Xmpp client started!") >> (processIncoming concurrently executeCommand(commandsQueue))

  private def processIncoming: Stream[F, Unit] =
    tcpClient.read.evalMap(stanza => Console[F].putStrLn(s"Receive: ${stanza.toString}"))
      .onComplete(
        Stream.eval(warn"Connection to XMPP server was closed! Retry after 3 Seconds") >> Stream.sleep(
          3 seconds
        ) >> processIncoming
      )

  override def executeCommand(commandsQueue: Queue[F, String]): Stream[F, Unit] =
    commandsQueue.dequeue.evalMap {
      case "Send data" =>
        val messageStanza: Message = Message(Sender("Me"), Receiver("Alice"), "test")
        tcpClient.send(messageStanza)
      case _ => Sync[F].delay(println("test")) >> ().pure[F]
    }
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
