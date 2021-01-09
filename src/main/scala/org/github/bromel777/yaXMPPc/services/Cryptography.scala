package org.github.bromel777.yaXMPPc.services

import java.security.{InvalidAlgorithmParameterException, NoSuchAlgorithmException, SecureRandom}

import cats.Applicative
import cats.effect.Sync
import org.bouncycastle.jcajce.provider.asymmetric.ecgost.{BCECGOST3410PrivateKey, BCECGOST3410PublicKey, KeyPairGeneratorSpi}
import org.bouncycastle.jce.interfaces.{ECPrivateKey, ECPublicKey}
import tofu.syntax.monadic._

trait Cryptography[F[_]] {

  def produceKeyPair: F[(BCECGOST3410PrivateKey, BCECGOST3410PublicKey)]
  def sign(privateKey: ECPrivateKey, msg: Array[Byte]): F[Array[Byte]]
  def verify(publicKey: ECPublicKey, msg: Array[Byte], signature: Unit): F[Boolean]
}

object Cryptography {

  def make[I[+_]: Applicative, F[_]: Sync]: I[Cryptography[F]] = (new Live[F]).pure[I]

  final private class Live[F[_]: Sync] extends Cryptography[F] {

    val DJB_TYPE = 0x05

    private val ECParamsSet = "Tc26-Gost-3410-12-256-paramSetA"

    override def produceKeyPair: F[(BCECGOST3410PrivateKey, BCECGOST3410PublicKey)] =
      Sync[F].delay {
        import org.bouncycastle.jcajce.spec.GOST3410ParameterSpec
        val ECGOST3410 = new KeyPairGeneratorSpi();
        val paramSpec  = new GOST3410ParameterSpec(ECParamsSet)

        try ECGOST3410.initialize(paramSpec, SecureRandom.getInstanceStrong)
        catch {
          case ignored @ (_: InvalidAlgorithmParameterException | _: NoSuchAlgorithmException) =>
        }
        val pair                               = ECGOST3410.generateKeyPair
        val privateKey: BCECGOST3410PrivateKey = pair.getPrivate.asInstanceOf[BCECGOST3410PrivateKey]
        val publicKey: BCECGOST3410PublicKey   = pair.getPublic.asInstanceOf[BCECGOST3410PublicKey]
        (privateKey, publicKey)
      }

    override def sign(privateKey: ECPrivateKey, msg: Array[Byte]): F[Array[Byte]] = Array(1: Byte).pure[F]

    override def verify(publicKey: ECPublicKey, msg: Array[Byte], signature: Unit): F[Boolean] = true.pure[F]
  }
}
