package org.github.bromel777.yaXMPPc.encryption

final class KuznyechikSchema[F[_]] private () extends EncryptionSchema[F] {

  override def encrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]] = ???

  override def decrypt(input: Array[Byte], key: Array[Byte]): F[Array[Byte]] = ???
}

object KuznyechikSchema {

  def make[F[_]]: EncryptionSchema[F] = ???
}
