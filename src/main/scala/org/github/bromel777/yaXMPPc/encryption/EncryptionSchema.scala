package org.github.bromel777.yaXMPPc.encryption

trait EncryptionSchema[F[_]] {

  def encrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]]
  def decrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]]
}