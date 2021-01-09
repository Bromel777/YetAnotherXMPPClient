package org.github.bromel777.yaXMPPc.programs

import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.Receiver

package object cli {

  sealed trait Command
  sealed trait XMPPClientCommand extends Command

  object XMPPClientCommand {
    case class SendMessage(receiver: Receiver, msg: String) extends XMPPClientCommand
    case object ProduceKeyPair extends XMPPClientCommand
    case object DeleteKeyPair extends XMPPClientCommand
    case object ProduceKeysForX3DH extends XMPPClientCommand
    case class InitSecureSession(receiver: Receiver) extends XMPPClientCommand
    case class Register(name: String) extends XMPPClientCommand
    case object Auth extends XMPPClientCommand
  }
  sealed trait XMPPServerCommand extends Command

  object XMPPServerCommand {
    case object ShowActiveSessions extends XMPPServerCommand
  }
  sealed trait CAServerCommands extends Command

  object CAServerCommands {
    case object ShowActivePublicKeys extends CAServerCommands
  }
}
