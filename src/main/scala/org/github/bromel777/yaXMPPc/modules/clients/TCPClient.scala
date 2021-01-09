package org.github.bromel777.yaXMPPc.modules.clients

import java.net.InetSocketAddress

import cats.syntax.traverse._
import cats.effect.concurrent.{MVar, MVar2}
import cats.effect.{Concurrent, ContextShift, Sync}
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.{Message, Receiver, Sender, Stanza}
import org.github.bromel777.yaXMPPc.modules.MessageSocket
import tofu.common.Console
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

final class TCPClient[F[_]: Concurrent: ContextShift: Logging: Console](
  host2connect: String,
  port2connect: Int,
  socketGroup: SocketGroup,
  queue2send: Queue[F, Stanza]
) extends Client[F, Stanza, Stanza] {

  import scodec.codecs.utf8

  override def send(out: Stanza): F[Unit] = queue2send.enqueue1(out) >> info"enqueue"

  private def processIncoming(socket: MessageSocket[F, String, String]): Stream[F, Stanza] =
    socket.read.map(Stanza.parseXML).unNone

  private def processOutgoing(socket: MessageSocket[F, String, String]): Stream[F, Unit] =
    //Stream.repeatEval(Console[F].readStrLn).map(_ => Stanza.toXML(Message(Sender("Me"), Receiver("Alice"), "test"))).evalTap(st => info"${st.toString}").evalMap(socket.write1)
    queue2send.dequeue.evalMap(stanza => info"write: ${stanza.toString}" >> socket.write1(Stanza.toXML(stanza)))

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
            processIncoming(socket) concurrently processOutgoing(socket)
          }
      }
}

object TCPClient {

  def make[F[_]: Concurrent: ContextShift: Logging: Console](
    host2connect: String,
    port2connect: Int,
    socketGroup: SocketGroup
  ): F[Client[F, Stanza, Stanza]] =
    Queue.bounded[F, Stanza](100).map { qu =>
      new TCPClient[F](host2connect, port2connect, socketGroup, qu)
    }
}
