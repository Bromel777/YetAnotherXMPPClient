package org.github.bromel777.yaXMPPc.modules.clients

import java.net.InetSocketAddress

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import monix.catnap.MVar
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import org.github.bromel777.yaXMPPc.modules.MessageSocket
import scodec.Codec
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class TCPClient[F[_]: Concurrent: ContextShift: Logging](
  host2connect: String,
  port2connect: Int,
  socketGroup: SocketGroup,
  openSocket: MVar[F, MessageSocket[F, String, String]]
) extends Client[F, Stanza, Stanza] {

  import scodec.codecs.utf8

  override def send(out: Stanza): F[Unit] =
    openSocket.isEmpty.ifM(
      openSocket.read.flatMap(_.write1(Stanza.toXML(out))),
      warn"Impossible to write to socket. Cause it's closed."
    )

  private def processIncoming(socket: MessageSocket[F, String, String]): Stream[F, Stanza] =
    socket.read.map(Stanza.parseXML).unNone

  override def read: Stream[F, Stanza] =
    Stream.eval(info"Start tcp connection to $host2connect:$port2connect") >>
    Stream
      .resource(socketGroup.client[F](new InetSocketAddress(host2connect, port2connect)))
      .flatMap { socket =>
        Stream.eval(info"Connected") >> Stream
          .eval(
            MessageSocket[F, String, String](
              socket,
              utf8,
              utf8,
              10
            )
          )
          .flatMap { socket =>
            Stream.eval(openSocket.put(socket)) >> processIncoming(socket)
          }
      }
}

object TCPClient {

  def make[F[_]: Concurrent: ContextShift: Logging](
    host2connect: String,
    port2connect: Int,
    socketGroup: SocketGroup
  ): F[Client[F, Stanza, Stanza]] =
    MVar.empty[F, MessageSocket[F, String, String]]().map { mvar =>
      new TCPClient[F](host2connect, port2connect, socketGroup, mvar)
    }
}
