package org.github.bromel777.yaXMPPc.domain.stanza

import scodec.Codec
import scodec.codecs.{uint4, Discriminated}
import cats.syntax.option._

sealed trait Stanza

final case class Iq() extends Stanza
final case class Presence() extends Stanza
final case class Message(sender: Sender, receiver: Receiver, value: String) extends Stanza

final case class Sender(value: String)
final case class Receiver(value: String)

object Stanza {

  import scala.xml._

  def toXML(stanza: Stanza): String = stanza match {
    case Iq() =>
      (<IQ>
      </IQ>).toString()
    case Presence() =>
      (<Presence>
      </Presence>).toString()
    case Message(sender, receiver, value) =>
      (<message from={sender.value} to={receiver.value}>
        <body>{value}</body>
      </message>).toString()
  }

  def parseXML(input: String): Option[Stanza] =
    scala.xml.XML.loadString(input) match {
      case iq if iq.label == "IQ" => Iq().some
      case presence if presence.label == "Presence" => Presence().some
      case message if message.label == "message" =>
        val sender = (message \ "@from").mkString
        val receiver = (message \ "@to").mkString
        val body = (message \\ "body").mkString
        Message(Sender(sender), Receiver(receiver), body).some
      case res =>
        println(s"Receive: ${res}")
        none
    }
}


