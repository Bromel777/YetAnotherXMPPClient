package org.github.bromel777.yaXMPPc.programs.xmpp

import java.security.{PrivateKey, PublicKey}

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import cats.syntax.option._
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPClientSettings
import org.github.bromel777.yaXMPPc.domain.xmpp.JId
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza._
import org.github.bromel777.yaXMPPc.modules.clients.{Client, TCPClient}
import org.github.bromel777.yaXMPPc.programs.Program
import org.github.bromel777.yaXMPPc.programs.cli.Command
import org.github.bromel777.yaXMPPc.programs.cli.XMPPClientCommand._
import org.github.bromel777.yaXMPPc.services.Cryptography
import org.github.bromel777.yaXMPPc.storage.Storage
import scorex.util.encode.Base64
import tofu.common.Console
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

final class XMPPClientProgram[F[_]: Concurrent: Console: Logging: Timer] private (
  settings: XMPPClientSettings,
  tcpClient: Client[F, Stanza, Stanza],
  cryptography: Cryptography[F],
  keysStorage: Storage[F, String, (PrivateKey, PublicKey)],
  x3dhCommonKeys: Storage[F, JId, Array[Byte]]
) extends Program[F] {

  var myJid: Option[String] = none

  override def run(commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    Stream.eval(info"Xmpp client started!") >> (processIncoming concurrently executeCommand(commandsQueue))

  private def processIncoming: Stream[F, Unit] =
    tcpClient.read
      .evalMap(handleStanza)
      .onComplete(
        Stream.eval(warn"Connection to XMPP server was closed! Retry after 3 Seconds") >> Stream.sleep(
          3 seconds
        ) >> processIncoming
      )

  private def handleStanza(stanza: Stanza): F[Unit] =
    stanza match {
      case iq: IqError => info"Receive error stanza: ${iq.errDesc}"
      case x3DHParams: IqX3DHParams =>
        keysStorage.get("Main key pair").flatMap {
          case Some(value) =>
            cryptography.produceCommonKeyBySender(value._1, x3DHParams.spkPub, x3DHParams.ikPub).flatMap {
              case (ekPub, secret) =>
                for {
                  cypherKey <- cryptography.kdf(secret, value._1, x3DHParams.ikPub)
                  _ <- x3dhCommonKeys.put(x3DHParams.JId, cypherKey)
                  _ <- tcpClient.send(IqX3dhFirstStep(JId(myJid.get), x3DHParams.JId, ekPub, value._2))
                } yield ()
            }
          case None => warn"Create main key pair and register it, before creation secure channel!"
        }
      case x3dhFirstStep: IqX3dhFirstStep =>
        keysStorage.get("Main key pair").flatMap {
          case Some((privateKeyI, _)) =>
            keysStorage.get("spk").flatMap {
              case Some((privateSpk, _)) =>
                for {
                  key <- cryptography.produceCommonKeyByReceiver(privateKeyI, privateSpk, x3dhFirstStep.ekPub, x3dhFirstStep.ikPub)
                  cypherKey <- cryptography.kdf(key, privateKeyI, x3dhFirstStep.ikPub)
                  _ <- x3dhCommonKeys.put(x3dhFirstStep.senderJID, cypherKey)
                } yield ()
              case None => error"Keys storage was damaged! Impossible to establish secure channel"
            }
          case None => error"Keys storage was damaged! Impossible to establish secure channel"
        }
      case Message(sender, _, msgText) =>
        x3dhCommonKeys.get(JId(sender.value)).flatMap {
          case Some(key) =>
            cryptography.decrypt(Base64.decode(msgText).get, key).flatMap {
              case Failure(exception) => info"Exception ${exception.getMessage} during decryption msg!"
              case Success(decrypt) => info"Receive message: ${new String(decrypt)} from ${sender.value}"
            }
          case None => info"Receive msg (by insecure channel): $msgText from ${sender.value}"
        }
      case _                         => info"Receive stanza: ${stanza.toString}"
    }

  override def executeCommand(commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    commandsQueue.dequeue.evalMap {
      case ProduceKeyPair =>
        cryptography.produceKeyPair.flatMap { keyPair =>
          trace"Main key pair produced!" >> keysStorage.put("Main key pair", keyPair)
        }
      case SendMessage(receiver, msg) =>
        x3dhCommonKeys.get(JId(receiver.value)) flatMap {
          case Some(value) => trace"Send msg by secure channel"
            cryptography.encrypt(msg.getBytes, value).flatMap { encrypt =>
              tcpClient.send(
                Message(
                  Sender(myJid.get),
                  receiver,
                  Base64.encode(encrypt)
                )
              )
            }
          case None => trace"Send msg by insecure channel"
        }
      case DeleteKeyPair =>
        trace"Delete main key pair from storage, if it's exist" >> keysStorage.delete("Main key pair")
      case InitSecureSession(receiverJid) =>
        trace"Trying to get x3dh params for user with jid ${receiverJid.value} from server" >>
        tcpClient.send(IqGetX3DHParams(receiverJid))
      case Register(name) =>
        for {
          _              <- trace"Going to register client with name: $name"
          mainKeyPairOpt <- keysStorage.get("Main key pair")
          mainKeyPair <- mainKeyPairOpt match {
                           case Some(value) => trace"Main key pair found." >> value.pure[F]
                           case None =>
                             cryptography.produceKeyPair.flatMap { keyPair =>
                               trace"Main key pair produced!" >> keysStorage.put("Main key pair", keyPair) >> keyPair
                                 .pure[F]
                             }
                         }
          registerStanza = IqRegister(name, mainKeyPair._2)
          _              = (myJid = name.some)
          _ <- tcpClient.send(registerStanza)
        } yield ()
      case Auth =>
        keysStorage.get("Main key pair").flatMap {
          case Some(value) =>
            val r   = new Random
            val msg = r.nextLong().toString.getBytes()
            for {
              signature <- cryptography.sign(value._1, msg)
              authStanza = IqAuth(msg, signature, value._2)
              _ <- tcpClient.send(authStanza)
            } yield ()
          case None => info"Couldn't find main key pair. Please create it and register!"
        }
      case ProduceKeysForX3DH =>
        for {
          mainKeyPairOpt <- keysStorage.get("Main key pair")
          mainKeyPair <- mainKeyPairOpt match {
                           case Some(value) => trace"Main key pair found." >> value.pure[F]
                           case None =>
                             cryptography.produceKeyPair.flatMap { keyPair =>
                               trace"Main key pair produced!" >> keysStorage.put("Main key pair", keyPair) >> keyPair
                                 .pure[F]
                             }
                         }
          _                  <- trace"Start producing keys for X3DH"
          spkPair            <- cryptography.produceKeyPair
          _                  <- trace"SPK Pair produced!"
          _                  <- keysStorage.put("spk", spkPair)
          spkPublicSignature <- cryptography.sign(mainKeyPair._1, spkPair._2.getEncoded)
          _                  <- trace"Create spkKey ${new String(spkPair._2.getEncoded)}, signature ${new String(spkPublicSignature)}"
          iqX3DH = IqX3DHInit(spkPair._2, mainKeyPair._2, spkPublicSignature)
          _ <- tcpClient.send(iqX3DH)
        } yield ()
      case _ => Sync[F].delay(println("test")) >> ().pure[F]
    }
}

object XMPPClientProgram {

  def make[F[_]: Concurrent: ContextShift: Timer](
    settings: XMPPClientSettings,
    socketGroup: SocketGroup,
    cryptography: Cryptography[F]
  )(implicit
    logs: Logs[F, F]
  ): F[Program[F]] =
    for {
      implicit0(log: Logging[F]) <- logs.forService[XMPPClientProgram[F]]
      keysStorage                <- Storage.makeMapStorage[F, String, (PrivateKey, PublicKey)]
      tcpClient                  <- TCPClient.make[F](settings.serverHost, settings.serverPort, socketGroup)
      x3dhStorage                <- Storage.makeMapStorage[F, JId, Array[Byte]]
    } yield new XMPPClientProgram(settings, tcpClient, cryptography, keysStorage, x3dhStorage)
}
