package org.github.bromel777.yaXMPPc.programs.xmpp

import java.util.UUID

import cats.effect.{Concurrent, ContextShift}
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPServerSettings
import org.github.bromel777.yaXMPPc.domain.sessions.XMPPServerSession
import org.github.bromel777.yaXMPPc.domain.user.UserInfo
import org.github.bromel777.yaXMPPc.domain.xmpp.JId
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.{Iq, Message, Presence, Stanza}
import org.github.bromel777.yaXMPPc.modules.servers.TCPServer
import org.github.bromel777.yaXMPPc.programs.Program
import org.github.bromel777.yaXMPPc.programs.cli.Command
import org.github.bromel777.yaXMPPc.storage.Storage
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class XMPPServerProgram[F[_]: Concurrent: ContextShift: Logging] private (
  settings: XMPPServerSettings,
  tcpServer: TCPServer[F],
  sessions: Map[UUID, XMPPServerSession],
  usersStorage: Storage[F, JId, UserInfo],
  stanzaBuffer: Storage[F, JId, Stanza]
) extends Program[F] {

  override def run(commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    Stream.eval(info"Xmpp server started!") >> tcpServer.receiverStream.evalMap(handleStanzaWithUUID)

  override def executeCommand(commandsQueue: Queue[F, Command]): Stream[F, Unit] = ???

  private def handleStanzaWithUUID(stanzaWithUUID: (Stanza, UUID)): F[Unit] =
    stanzaWithUUID match {
      case (message: Message, uuid) =>
        sessions.find(_._2.jid.contains(JId(message.receiver.value))) match {
          case Some((sessionUUId, session)) =>
            trace"Receive msg stanza from ${message.sender.value} to ${message.receiver.value}. Going to send msg to receiver (Session uuid: ${sessionUUId})." >> tcpServer.send(message, sessionUUId)
          case None =>
            trace"Receive msg stanza from ${message.sender.value} to ${message.receiver.value}. But there is no one active session with receiver. Put msg to buffer"
        }
      case (iq: Iq, uuid)             =>
        trace"Receive iq stanza: ${iq.toString} from ${uuid.toString}"
      case (presence: Presence, uuid) =>
        trace"Receive presence stanza: ${presence.toString} from ${uuid.toString}"
      case (_, uuid)                  => warn"Unknown stanza from ${uuid.toString}"
    }
}

object XMPPServerProgram {

  def make[F[_]: Concurrent: ContextShift](settings: XMPPServerSettings, socketGroup: SocketGroup)(implicit
    logs: Logs[F, F]
  ): F[Program[F]] =
    for {
      implicit0(logging: Logging[F]) <- logs.forService[XMPPServerProgram[F]]
      server <- TCPServer.make[F](settings, socketGroup)
      sessionsMap = Map.empty[UUID, XMPPServerSession]
      usersStorage <- Storage.makeMapStorage[F, JId, UserInfo]
      stanzaBuffer <- Storage.makeMapStorage[F, JId, Stanza]
    } yield new XMPPServerProgram(settings, server, sessionsMap, usersStorage, stanzaBuffer)
}
