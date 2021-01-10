package org.github.bromel777.yaXMPPc.services

import java.security._

import cats.Applicative
import cats.effect.Sync
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Cipher, KeyAgreement}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jcajce.provider.symmetric.GOST3412_2015
import java.security.Security
import scorex.crypto.hash.Sha256
import scorex.crypto.hash.Stribog256._
import tofu.syntax.monadic._

trait Cryptography[F[_]] {

  def produceKeyPair: F[(PrivateKey, PublicKey)]
  def sign(privateKey: PrivateKey, msg: Array[Byte]): F[Array[Byte]]
  def verify(publicKey: PublicKey, msg: Array[Byte], signature: Array[Byte]): F[Boolean]
  def kdf(previousKey: Array[Byte], privateKey: PrivateKey, publicKey: PublicKey): F[Array[Byte]]
  def encrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]]
  def decrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]]

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

    override def kdf(previousKey: Array[Byte], privateKey: PrivateKey, publicKey: PublicKey): F[Array[Byte]] =
      Sync[F].delay {

        val kXorOpad: Array[Byte] = previousKey.map(elem => (elem ^ 0x36).toByte)
        val kXorIpad: Array[Byte] = previousKey.map(elem => (elem ^ 0x5c).toByte)
        val keyAgree              = KeyAgreement.getInstance("ECDH")
        keyAgree.init(privateKey)
        keyAgree.doPhase(publicKey, true)
        val orLeftRes: Array[Byte]     = kXorIpad.zip(keyAgree.generateSecret()).map { case (b1, b2) => (b1 | b2).toByte }
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
        keyAgree.init(ePrivateKey)
        keyAgree.doPhase(ipk, true)
        val dh2 = keyAgree.generateSecret()
        keyAgree.init(ePrivateKey)
        keyAgree.doPhase(spk, true)
        val dh3 = keyAgree.generateSecret()
        val key = Sha256.hash(dh1 ++ dh2 ++ dh3)
        ePublicKey -> key
      }

    override def produceCommonKeyByReceiver(
      receiverPrivateKey: PrivateKey,
      receiverSPKPrivate: PrivateKey,
      senderEpPub: PublicKey,
      senderIkPub: PublicKey
    ): F[Array[Byte]] =
      Sync[F].delay {
        val f        = KeyPairGenerator.getInstance("ECDH", "BC")
        val keyAgree = KeyAgreement.getInstance("ECDH")
        keyAgree.init(receiverSPKPrivate)
        keyAgree.doPhase(senderIkPub, true)
        val dh1 = keyAgree.generateSecret()
        keyAgree.init(receiverPrivateKey)
        keyAgree.doPhase(senderEpPub, true)
        val dh2 = keyAgree.generateSecret()
        keyAgree.init(receiverSPKPrivate)
        keyAgree.doPhase(senderEpPub, true)
        val dh3 = keyAgree.generateSecret()
        val key = Sha256.hash(dh1 ++ dh2 ++ dh3)
        key
      }

    override def encrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]] =
      Sync[F].delay {
        val cipher: Cipher = Cipher.getInstance("GOST3412-2015/CBC/PKCS5Padding", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "GOST3412-2015"))
        cipher.doFinal(input)
      }

    override def decrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]] =
      Sync[F].delay {
        val cipher: Cipher = Cipher.getInstance("GOST3412-2015/CBC/PKCS5Padding", "BC")
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "GOST3412-2015"))
        cipher.doFinal(input)
      }
  }
}
