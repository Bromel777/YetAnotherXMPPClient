package org.github.bromel777.yaXMPPc.modules

import cats.effect.Concurrent
import cats.effect.std.Queue
import fs2.Stream
import fs2.io.tcp.Socket
import scodec.stream.{StreamDecoder, StreamEncoder}
import scodec.{Decoder, Encoder}
import tofu.syntax.monadic._

trait MessageSocket[F[_], In, Out] {
  def read: Stream[F, In]
  def write1(out: Out): F[Unit]
}

object MessageSocket {

  def apply[F[_]: Concurrent, In, Out](
    socket: Socket[F],
    inDecoder: Decoder[In],
    outEncoder: Encoder[Out],
    outputBound: Int
  ): F[MessageSocket[F, In, Out]] =
    for {
      outgoing <- Queue.bounded[F, Out](outputBound)
    } yield new MessageSocket[F, In, Out] {

      def read: Stream[F, In] = {
        val readSocket = socket
          .reads(1024)
          .through(StreamDecoder.many(inDecoder).toPipeByte[F])

        val writeOutput = Stream.eval(outgoing.take)
          .through(StreamEncoder.many(outEncoder).toPipeByte)
          .through(socket.writes(None))

        readSocket.concurrently(writeOutput)
      }
      def write1(out: Out): F[Unit] = outgoing.offer(out)
    }
}
