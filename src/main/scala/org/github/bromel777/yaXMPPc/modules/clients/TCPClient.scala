package org.github.bromel777.yaXMPPc.modules.clients

import java.net.InetSocketAddress

import cats.effect.{Concurrent, ContextShift, Sync}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import org.github.bromel777.yaXMPPc.modules.MessageSocket
import tofu.logging.Logging
import tofu.syntax.logging._

final class TCPClient[F[_]: Concurrent: ContextShift: Logging](
  host2connect: String,
  port2connect: Int,
  socketGroup: SocketGroup
) extends Client[F, Stanza, Stanza] {

  override def receiverStream: Stream[F, Stanza] = ???

  override def send(out: Stanza): F[Unit] = ???

  private def processIncoming(socket: MessageSocket[F, Stanza, Stanza]): Stream[F, Unit] =
    socket.read.evalMap(test => Sync[F].delay(println(s"Hello! $test")))

  override def start: Stream[F, Unit] =
    Stream.eval(info"Start tcp connection to $host2connect:$port2connect") >>
    Stream
      .resource(socketGroup.client[F](new InetSocketAddress(host2connect, port2connect)))
      .flatMap { socket =>
        Stream.eval(info"Connected") ++ Stream
          .eval(
            MessageSocket[F, Stanza, Stanza](
              socket,
              Stanza.stanzaCodec.asDecoder,
              Stanza.stanzaCodec.asEncoder,
              10
            )
          )
          .flatMap { socket =>
            processIncoming(socket)
          }
      }
}

object TCPClient {

  def make[F[_]: Concurrent: ContextShift: Logging](
    host2connect: String,
    port2connect: Int,
    socketGroup: SocketGroup
  ): Client[F, Stanza, Stanza] =
    new TCPClient[F](host2connect, port2connect, socketGroup)
}
