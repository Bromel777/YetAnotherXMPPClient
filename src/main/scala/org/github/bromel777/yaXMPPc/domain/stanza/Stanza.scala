package org.github.bromel777.yaXMPPc.domain.stanza

import scodec.Codec
import scodec.codecs.{uint4, Discriminated}

sealed trait Stanza

final case class Iq() extends Stanza
final case class Presence() extends Stanza
final case class Message(sender: Sender, receiver: Receiver) extends Stanza

final case class Sender(value: String)
final case class Receiver(value: String)

object Stanza {

  import scodec.codecs.implicits._

  implicit def stanzasVals = Discriminated[Stanza, Int](uint4)
  implicit def iqVal = stanzasVals.bind[Iq](0)
  implicit def presenceVal = stanzasVals.bind[Presence](1)
  implicit def msgVal = stanzasVals.bind[Message](2)

  val senderCodec = Codec[Sender]
  val receiverCode = Codec[Receiver]
  val stanzaCodec          = Codec[Stanza]
}


