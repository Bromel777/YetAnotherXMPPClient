package org.github.bromel777.yaXMPPc.services

import java.security._

import cats.Applicative
import cats.effect.Sync
import javax.crypto.KeyAgreement
import org.bouncycastle.jce.provider.BouncyCastleProvider
import scorex.crypto.hash.Sha256
import scorex.crypto.hash.Stribog256._
import scorex.util.encode.Base64
import tofu.syntax.monadic._

trait Cryptography[F[_]] {

  def produceKeyPair: F[(PrivateKey, PublicKey)]
  def sign(privateKey: PrivateKey, msg: Array[Byte]): F[Array[Byte]]
  def verify(publicKey: PublicKey, msg: Array[Byte], signature: Array[Byte]): F[Boolean]
  def kdf(previousKey: Array[Byte], input: Array[Byte]): F[Array[Byte]]

  def produceCommonKeyBySender(
    senderPrivateKey: PrivateKey,
    spk: PublicKey,
    ipk: PublicKey
  ): F[(PublicKey, Array[Byte])]

  def produceCommonKeyByReceiver(
    receiverPrivateKey: PrivateKey,
    receiverSPKPrivate: PrivateKey,
    senderEpPub: PublicKey,
    senderIkPub: PublicKey
  ): F[Array[Byte]]
}

object Cryptography {

  def make[I[+_]: Applicative, F[_]: Sync]: I[Cryptography[F]] = (new Live[F]).pure[I]

  final private class Live[F[_]: Sync] extends Cryptography[F] {

    Security.addProvider(new BouncyCastleProvider());

    override def produceKeyPair: F[(PrivateKey, PublicKey)] =
      Sync[F].delay {
        val f                      = KeyPairGenerator.getInstance("ECDH", "BC");
        val pair                   = f.generateKeyPair
        val privateKey: PrivateKey = pair.getPrivate
        val publicKey: PublicKey   = pair.getPublic
        (privateKey, publicKey)
      }

    override def sign(privateKey: PrivateKey, msg: Array[Byte]): F[Array[Byte]] =
      Sync[F].delay {
        import java.security.Signature
        val s = Signature.getInstance("SHA256withECDSA", "BC")
        s.initSign(privateKey)
        s.update(msg)
        s.sign()
      }

    override def verify(publicKey: PublicKey, msg: Array[Byte], signature: Array[Byte]): F[Boolean] =
      Sync[F].delay {
        val s = Signature.getInstance("SHA256withECDSA", "BC")
        s.initVerify(publicKey)
        s.update(msg)
        s.verify(signature)
      }

    override def kdf(previousKey: Array[Byte], input: Array[Byte]): F[Array[Byte]] =
      Sync[F].delay {
        val kXorOpad: Array[Byte] = previousKey.map(elem => (elem ^ 0x36).toByte)
        val kXorIpad: Array[Byte] = previousKey.map(elem => (elem ^ 0x5c).toByte)

        val orLeftRes: Array[Byte]     = kXorIpad.zip(input).map { case (b1, b2) => (b1 | b2).toByte }
        val orLeftResHash: Array[Byte] = hash(orLeftRes).array

        val leftOrRight = kXorOpad.zip(orLeftResHash).map { case (b1, b2) => (b1 | b2).toByte }

        hash(leftOrRight).array
      }

    override def produceCommonKeyBySender(
      senderPrivateKey: PrivateKey,
      spk: PublicKey,
      ipk: PublicKey
    ): F[(PublicKey, Array[Byte])] =
      Sync[F].delay {
        val f                       = KeyPairGenerator.getInstance("ECDH", "BC")
        val keyAgree                = KeyAgreement.getInstance("ECDH")
        val pair                    = f.generateKeyPair
        val ePrivateKey: PrivateKey = pair.getPrivate
        val ePublicKey: PublicKey   = pair.getPublic
        keyAgree.init(senderPrivateKey)
        keyAgree.doPhase(spk, true)
        val dh1 = keyAgree.generateSecret()
        println(s"DH1: ${Base64.encode(dh1)}")
        keyAgree.init(ePrivateKey)
        keyAgree.doPhase(ipk, true)
        val dh2 = keyAgree.generateSecret()
        println(s"DH2: ${Base64.encode(dh2)}")
        keyAgree.init(ePrivateKey)
        keyAgree.doPhase(spk, true)
        val dh3 = keyAgree.generateSecret()
        println(s"DH3: ${Base64.encode(dh3)}")
        val key = Sha256.hash(dh1 ++ dh2 ++ dh3)
        println(Base64.encode(key))
        ePublicKey -> key
      }

    override def produceCommonKeyByReceiver(
      receiverPrivateKey: PrivateKey,
      receiverSPKPrivate: PrivateKey,
      senderEpPub: PublicKey,
      senderIkPub: PublicKey
    ): F[Array[Byte]] =
      Sync[F].delay {
        val f                       = KeyPairGenerator.getInstance("ECDH", "BC")
        val keyAgree                = KeyAgreement.getInstance("ECDH")
        keyAgree.init(receiverSPKPrivate)
        keyAgree.doPhase(senderIkPub, true)
        val dh1 = keyAgree.generateSecret()
        println(s"DH1: ${Base64.encode(dh1)}")
        keyAgree.init(receiverPrivateKey)
        keyAgree.doPhase(senderEpPub, true)
        val dh2 = keyAgree.generateSecret()
        println(s"DH2: ${Base64.encode(dh2)}")
        keyAgree.init(receiverSPKPrivate)
        keyAgree.doPhase(senderEpPub, true)
        val dh3 = keyAgree.generateSecret()
        println(s"DH3: ${Base64.encode(dh3)}")
        val key = Sha256.hash(dh1 ++ dh2 ++ dh3)
        println(Base64.encode(key))
        key
      }
  }
}
