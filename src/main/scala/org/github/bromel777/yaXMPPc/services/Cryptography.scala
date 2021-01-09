package org.github.bromel777.yaXMPPc.services

import java.security._

import cats.Applicative
import cats.effect.Sync
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import tofu.syntax.monadic._

trait Cryptography[F[_]] {

  def produceKeyPair: F[(PrivateKey, PublicKey)]
  def sign(privateKey: PrivateKey, msg: Array[Byte]): F[Array[Byte]]
  def verify(publicKey: PublicKey, msg: Array[Byte], signature: Array[Byte]): F[Boolean]
}

object Cryptography {

  def make[I[+_]: Applicative, F[_]: Sync]: I[Cryptography[F]] = (new Live[F]).pure[I]

  final private class Live[F[_]: Sync] extends Cryptography[F] {

    Security.addProvider(new BouncyCastleProvider());

    override def produceKeyPair: F[(PrivateKey, PublicKey)] =
      Sync[F].delay {
        val f = KeyPairGenerator.getInstance("GOST3410", "BC")
        val pair                   = f.generateKeyPair
        val privateKey: PrivateKey = pair.getPrivate
        val publicKey: PublicKey   = pair.getPublic
        (privateKey, publicKey)
      }

    override def sign(privateKey: PrivateKey, msg: Array[Byte]): F[Array[Byte]] =
      Sync[F].delay {
        import java.security.Signature
        val s = Signature.getInstance("GOST3410", "BC")
        s.initSign(privateKey)
        s.update(msg)
        s.sign()
      }

    override def verify(publicKey: PublicKey, msg: Array[Byte], signature: Array[Byte]): F[Boolean] =
      Sync[F].delay {
        val s = Signature.getInstance("GOST3410", "BC")
        s.initVerify(publicKey)
        s.update(msg)
        s.verify(signature)
      }
  }
}
