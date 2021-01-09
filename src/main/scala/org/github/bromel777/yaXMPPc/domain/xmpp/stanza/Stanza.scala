package org.github.bromel777.yaXMPPc.domain.xmpp.stanza

import java.security.spec.X509EncodedKeySpec
import java.security.{KeyFactory, PublicKey}
import java.util.Base64

import cats.syntax.option._

sealed trait Stanza

trait Iq extends Stanza

case class IqX3DHInit(spkPub: PublicKey, ikPub: PublicKey, sig: Array[Byte]) extends Iq
case class IqRegister(name: String, publicKey: PublicKey) extends Iq
case class IqAuth(str: Array[Byte], signature: Array[Byte], publicKey: PublicKey) extends Iq
case class IqError(errDesc: String) extends Iq
case object UnknownIq extends Iq
final case class Presence() extends Stanza
final case class Message(sender: Sender, receiver: Receiver, value: String) extends Stanza

final case class Sender(value: String)
final case class Receiver(value: String)

object Stanza {

  val encoder = Base64.getEncoder
  val decoder = Base64.getDecoder

  def toXML(stanza: Stanza): String =
    stanza match {
      case UnknownIq =>
        (<iq></iq>).toString()
      case IqX3DHInit(spkPub, ikPub, sig) =>
        (
          <iq type="set">
          <x3dhmirea xmlns="mirea:diplom:x3dh">
            <SPK>
              {encoder.encodeToString(spkPub.getEncoded)}
            </SPK>
            <ikpub>
              {encoder.encodeToString(ikPub.getEncoded)}
            </ikpub>
            <sig>
              {encoder.encodeToString(sig)}
            </sig>
          </x3dhmirea>
        </iq>
        ).toString()
      case IqAuth(str, signature, publicKey) =>
        <iq type="set">
        <auth xmlns="mirea:diplom:auth">
        <str>{encoder.encode(str)}</str>
        <signature>{encoder.encodeToString(signature)}</signature>
        <publickey>{encoder.encodeToString(publicKey.getEncoded)}</publickey>
        </auth>
        </iq>.toString()
      case IqRegister(name, publicKey) =>
        <iq type="set">
        <register xmlns="mirea:diplom:register">
        <name>{name}</name>
        <publickey>{encoder.encodeToString(publicKey.getEncoded)}</publickey>
        </register>
        </iq>.toString()
      case IqError(errDesc) =>
        <iq type="error"><error>{errDesc}</error></iq>.toString()
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
      case iq if iq.label == "iq" && (iq \\ "x3dhmirea").nonEmpty =>
        println(s"Iq: $iq")
        val x3dh        = iq \\ "x3dhmirea"
        val spkPubBytes = Base64.getDecoder.decode((x3dh \\ "SPK").text.trim)
        val ikPubByte   = decoder.decode((x3dh \\ "ikpub").text.trim)
        val sigByte     = decoder.decode((x3dh \\ "sig").text.trim)
        val spkPub      = KeyFactory.getInstance("GOST3410", "BC").generatePublic(new X509EncodedKeySpec(spkPubBytes))
        val ikPub       = KeyFactory.getInstance("GOST3410", "BC").generatePublic(new X509EncodedKeySpec(ikPubByte))
        IqX3DHInit(
          spkPub,
          ikPub,
          sigByte
        ).some
      case iqAuth if iqAuth.label == "iq" && (iqAuth \\ "auth").nonEmpty =>
        val auth           = iqAuth \\ "auth"
        val str            = decoder.decode((auth \\ "str").text)
        val signature      = decoder.decode((auth \\ "signature").text.trim)
        val publicKeyBytes = decoder.decode((auth \\ "publickey").text.trim)
        val publicKey      = KeyFactory.getInstance("GOST3410", "BC").generatePublic(new X509EncodedKeySpec(publicKeyBytes))
        IqAuth(str, signature, publicKey).some
      case iqRegister if iqRegister.label == "iq" && (iqRegister \\ "register").nonEmpty =>
        val register = iqRegister \\ "register"
        val name = (iqRegister \\ "name").text
        val publicKeyBytes = decoder.decode((register \\ "publickey").text.trim)
        val publicKey      = KeyFactory.getInstance("GOST3410", "BC").generatePublic(new X509EncodedKeySpec(publicKeyBytes))
        IqRegister(name, publicKey).some
      case iqError if iqError.label == "iq" && (iqError \\ "error").nonEmpty =>
        val errorDesc = iqError \\ "error"
        IqError(errorDesc.text).some
      case iqUn if iqUn.label == "iq"               => UnknownIq.some
      case presence if presence.label == "Presence" => Presence().some
      case message if message.label == "message" =>
        val sender   = (message \ "@from").mkString
        val receiver = (message \ "@to").mkString
        val body     = (message \\ "body").mkString
        Message(Sender(sender), Receiver(receiver), body).some
      case res =>
        println(s"Receive: $res. ${(res \\ "x3dhmirea").isEmpty}")
        none
    }
}
