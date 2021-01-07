package org.github.bromel777.yaXMPPc.modules.clients

import java.net.InetSocketAddress

import cats.effect.Concurrent
import fs2.Stream
import fs2.io.Network
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class TCPClient[F[_]: Concurrent: Network: Logging](host2connect: String, port2connect: Int, socketGroup: SocketGroup) extends Client[F, Array[Byte], Array[Byte]]{

  override def receiverStream: Stream[F, Array[Byte]] = ???

  override def send(out: Array[Byte]): F[Unit] = ???

  override def start: Stream[F, Unit] =
    Stream.eval(info"Start tcp connection to $host2connect:$port2connect") >>
      Stream.resource(socketGroup.client[F](new InetSocketAddress(host2connect, port2connect)))
        .flatMap { socket =>
          Stream.eval(info"Connected") ++ Stream.eval(
            MessageSocket[F, Stanza, Stanza](

            )
          )
        }
}

object TCPClient {

  def make[F[_]: Logging](host2connect: String, port2connect: Int): Client[F, Array[Byte], Array[Byte]] =
    new TCPClient[F](host2connect, port2connect)
}
