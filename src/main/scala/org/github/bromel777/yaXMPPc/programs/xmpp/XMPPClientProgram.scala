package org.github.bromel777.yaXMPPc.programs.xmpp

import java.security.{PrivateKey, PublicKey}

import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPClientSettings
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.{IqX3DHInit, Stanza}
import org.github.bromel777.yaXMPPc.modules.clients.{Client, TCPClient}
import org.github.bromel777.yaXMPPc.programs.Program
import org.github.bromel777.yaXMPPc.programs.cli.Command
import org.github.bromel777.yaXMPPc.programs.cli.XMPPClientCommand.{DeleteKeyPair, ProduceKeyPair, ProduceKeysForX3DH}
import org.github.bromel777.yaXMPPc.services.Cryptography
import org.github.bromel777.yaXMPPc.storage.Storage
import tofu.common.Console
import tofu.logging.{Logging, Logs}
import tofu.syntax.logging._
import tofu.syntax.monadic._

import scala.concurrent.duration._

final class XMPPClientProgram[F[_]: Concurrent: Console: Logging: Timer] private (
  settings: XMPPClientSettings,
  tcpClient: Client[F, Stanza, Stanza],
  cryptography: Cryptography[F],
  keysStorage: Storage[F, String, (PrivateKey, PublicKey)]
) extends Program[F] {

  override def run(commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    Stream.eval(info"Xmpp client started!") >> (processIncoming concurrently executeCommand(commandsQueue))

  private def processIncoming: Stream[F, Unit] =
    tcpClient.read
      .evalMap(stanza => Console[F].putStrLn(s"Receive: ${stanza.toString}"))
      .onComplete(
        Stream.eval(warn"Connection to XMPP server was closed! Retry after 3 Seconds") >> Stream.sleep(
          3 seconds
        ) >> processIncoming
      )

  override def executeCommand(commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    commandsQueue.dequeue.evalMap {
      case ProduceKeyPair =>
        cryptography.produceKeyPair.flatMap { keyPair =>
          trace"Main key pair produced!" >> keysStorage.put("Main key pair", keyPair)
        }
      case DeleteKeyPair =>
        trace"Delete main key pair from storage, if it's exist" >> keysStorage.delete("Main key pair")
      case ProduceKeysForX3DH =>
        for {
          mainKeyPairOpt <- keysStorage.get("Main key pair")
          mainKeyPair <- mainKeyPairOpt match {
            case Some(value) => value.pure[F]
            case None => cryptography.produceKeyPair.flatMap { keyPair =>
              trace"Main key pair produced!" >> keysStorage.put("Main key pair", keyPair) >> keyPair.pure[F]
            }
          }
          _ <- trace"Start producing keys for X3DH"
          spkPair <- cryptography.produceKeyPair
          _ <- trace"SPK Pair produced!"
          _ <- keysStorage.put("spk", spkPair)
          spkPublicSignature <- cryptography.sign(mainKeyPair._1, spkPair._2.getEncoded)
          _ <- trace"Create spkKey ${new String(spkPair._2.getEncoded)}, signature ${new String(spkPublicSignature)}"
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
      keysStorage <- Storage.makeMapStorage[F, String, (PrivateKey, PublicKey)]
      tcpClient <- TCPClient.make[F](settings.serverHost, settings.serverPort, socketGroup)
    } yield new XMPPClientProgram(settings, tcpClient, cryptography, keysStorage)
}
