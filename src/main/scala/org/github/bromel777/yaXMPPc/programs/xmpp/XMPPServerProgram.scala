package org.github.bromel777.yaXMPPc.programs.xmpp

import java.util.UUID

import cats.effect.{Concurrent, ContextShift}
import cats.syntax.option._
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPServerSettings
import org.github.bromel777.yaXMPPc.domain.sessions.XMPPServerSession
import org.github.bromel777.yaXMPPc.domain.user.UserInfo
import org.github.bromel777.yaXMPPc.domain.xmpp.JId
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza._
import org.github.bromel777.yaXMPPc.modules.servers.TCPServer
import org.github.bromel777.yaXMPPc.programs.Program
import org.github.bromel777.yaXMPPc.programs.cli.Command
import org.github.bromel777.yaXMPPc.services.Cryptography
import org.github.bromel777.yaXMPPc.storage.Storage
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class XMPPServerProgram[F[_]: Concurrent: ContextShift: Logging] private (
  settings: XMPPServerSettings,
  tcpServer: TCPServer[F],
  sessions: Storage[F, UUID, XMPPServerSession],
  usersStorage: Storage[F, JId, UserInfo],
  stanzaBuffer: Storage[F, JId, Stanza],
  x3dhStanzas: Storage[F, JId, IqX3DHInit],
  cryptography: Cryptography[F]
) extends Program[F] {

  override def run(commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    Stream.eval(info"Xmpp server started!") >> tcpServer.receiverStream.evalMap(handleStanzaWithUUID)

  override def executeCommand(commandsQueue: Queue[F, Command]): Stream[F, Unit] = ???

  private def handleStanzaWithUUID(stanzaWithUUID: (Stanza, UUID)): F[Unit] =
    stanzaWithUUID match {
      case (message: Message, uuid) =>
        sessions.find { case (_, value) => value.jid.contains(JId(message.receiver.value)) }.flatMap {
          case Some((sessionUUId, session)) =>
            trace"Receive msg stanza from ${message.sender.value} to ${message.receiver.value}. Going to send msg to receiver (Session uuid: $sessionUUId)." >> tcpServer
              .send(message, sessionUUId)
          case None =>
            trace"Receive msg stanza from ${message.sender.value} to ${message.receiver.value}. But there is no one active session with receiver. Put msg to buffer"
        }
      case (x3DHInit: IqX3DHInit, uuid) =>
        for {
          _ <- trace"Trying to register x3dh for user with uuid $uuid"
          _ <- sessions.get(uuid) flatMap {
                 case Some(XMPPServerSession(Some(jid))) =>
                   x3dhStanzas.put(jid, x3DHInit) >> trace"Registration x3dh params completed!"
                 case _ =>
                   val errorStanza = IqError("Register your jid before publishing x3dh init msg")
                   tcpServer.send(errorStanza, uuid)
               }
        } yield ()
      case (iqRegister: IqRegister, uuid) =>
        trace"Try to register user by name ${iqRegister.name}" >> usersStorage
          .contains(JId(s"${iqRegister.name}@diplom"))
          .ifM(
            tcpServer.send(IqError(s"User with name ${iqRegister.name} already exist"), uuid),
            for {
              _ <- usersStorage.put(JId(s"${iqRegister.name}@diplom"), UserInfo(iqRegister.publicKey))
              _ <- sessions.put(uuid, XMPPServerSession(JId(iqRegister.name).some))
              _ <- usersStorage.put(JId(iqRegister.name), UserInfo(iqRegister.publicKey))
            } yield ()
          )
      case (iqAuth: IqAuth, uuid) =>
        for {
          _              <- trace"Trying to auth user with uuid ${uuid.toString}"
          signatureCheck <- cryptography.verify(iqAuth.publicKey, iqAuth.str, iqAuth.signature)
          _ <- if (signatureCheck)
                 usersStorage
                   .find { case (_, info) => info.publicKey.getEncoded sameElements iqAuth.publicKey.getEncoded }
                   .flatMap {
                     case Some((jid, _)) =>
                       trace"Auth user with uuid ${uuid.toString} and jid ${jid.value}" >> sessions.put(
                         uuid,
                         XMPPServerSession(jid.some)
                       )
                     case None =>
                       trace"Auth for unknown user!" >> tcpServer.send(IqError("Please register before auth"), uuid)
                   }
               else trace"Signature check failed!"
        } yield ()
      case (iq: Iq, uuid) =>
        trace"Receive iq stanza: ${iq.toString} from ${uuid.toString}"
      case (presence: Presence, uuid) =>
        trace"Receive presence stanza: ${presence.toString} from ${uuid.toString}"
      case (_, uuid) => warn"Unknown stanza from ${uuid.toString}"
    }
}

object XMPPServerProgram {

  def make[F[_]: Concurrent: ContextShift](
    settings: XMPPServerSettings,
    socketGroup: SocketGroup,
    cryptography: Cryptography[F]
  )(implicit
    logs: Logs[F, F]
  ): F[Program[F]] =
    for {
      implicit0(logging: Logging[F]) <- logs.forService[XMPPServerProgram[F]]
      server                         <- TCPServer.make[F](settings, socketGroup)
      sessionsMap                    <- Storage.makeMapStorage[F, UUID, XMPPServerSession]
      usersStorage                   <- Storage.makeMapStorage[F, JId, UserInfo]
      stanzaBuffer                   <- Storage.makeMapStorage[F, JId, Stanza]
      x3dhStorage                    <- Storage.makeMapStorage[F, JId, IqX3DHInit]
    } yield new XMPPServerProgram(settings, server, sessionsMap, usersStorage, stanzaBuffer, x3dhStorage, cryptography)
}
