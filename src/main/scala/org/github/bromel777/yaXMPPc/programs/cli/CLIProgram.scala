package org.github.bromel777.yaXMPPc.programs.cli

import fs2.Stream
import fs2.concurrent.Queue
import org.github.bromel777.yaXMPPc.domain.xmpp.JId
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.Receiver
import org.github.bromel777.yaXMPPc.programs.cli.CAServerCommands.ShowActivePublicKeys
import org.github.bromel777.yaXMPPc.programs.cli.XMPPClientCommand._
import org.github.bromel777.yaXMPPc.programs.cli.XMPPServerCommand.ShowActiveSessions
import tofu.common.Console

final class CLIProgram[F[_]: Console](commandsQueue: Queue[F, Command]) {

  def run: Stream[F, Unit] =
    Stream
      .repeatEval(Console[F].readStrLn)
      .map(parseCommand)
      .evalMap(commandsQueue.enqueue1)

  private def parseCommand(input: String): Command =
    input.split(" ").toList match {
      case "SendMessage" :: receiver :: msg :: Nil => SendMessage(Receiver(receiver), msg)
      case "ProduceKeyPair" :: Nil                 => ProduceKeyPair
      case "DeleteKeyPair" :: Nil                  => DeleteKeyPair
      case "ProduceKeysForX3DH" :: Nil             => ProduceKeysForX3DH
      case "InitSecureSession" :: receiver :: Nil  => InitSecureSession(JId(receiver))
      case "Register" :: name :: Nil               => Register(name)
      case "Auth" :: Nil                           => Auth
      case "ShowActiveSessions" :: Nil             => ShowActiveSessions
      case "ShowActivePublicKeys" :: Nil           => ShowActivePublicKeys
    }
}

object CLIProgram {

  def make[F[_]: Console](commandsQueue: Queue[F, Command]): Stream[F, Unit] =
    new CLIProgram(commandsQueue).run
}
