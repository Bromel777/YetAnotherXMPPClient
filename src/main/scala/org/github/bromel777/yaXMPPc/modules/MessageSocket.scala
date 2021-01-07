package org.github.bromel777.yaXMPPc.modules

import cats.effect.Concurrent
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.Socket
import scodec.stream.{CodecError, StreamDecoder, StreamEncoder}
import scodec.{Decoder, Encoder}
import tofu.logging.Logging
import tofu.syntax.logging._
import tofu.syntax.monadic._

trait MessageSocket[F[_], In, Out] {
  def read: Stream[F, In]
  def write1(out: Out): F[Unit]
}

object MessageSocket {

  def apply[F[_]: Concurrent: Logging, In, Out](
    socket: Socket[F],
    inDecoder: Decoder[In],
    outEncoder: Encoder[Out],
    outputBound: Int
  ): F[MessageSocket[F, In, Out]] =
    for {
      outgoing <- Queue.bounded[F, Out](outputBound)
    } yield new MessageSocket[F, In, Out] {

      def read: Stream[F, In] = {
        def readSocket: Stream[F, In] =
          socket
            .reads(1024)
            .through(StreamDecoder.many(inDecoder).toPipeByte[F])
            .handleErrorWith {
              case codecError: CodecError =>
                Stream.eval(
                  error"Error during decoding message from Array[Byte] to Stanza. Message: ${codecError.err.message}. Continue reading"
                ) >> readSocket
              case err =>
                Stream.eval(error"Unhandled exception. Message ${err.getMessage}. Abort connection!") >> Stream.empty
            }

        val writeOutput = outgoing.dequeue
          .through(StreamEncoder.many(outEncoder).toPipeByte)
          .through(socket.writes(None))

        readSocket.concurrently(writeOutput)
      }
      def write1(out: Out): F[Unit] = outgoing.enqueue1(out)
    }
}
