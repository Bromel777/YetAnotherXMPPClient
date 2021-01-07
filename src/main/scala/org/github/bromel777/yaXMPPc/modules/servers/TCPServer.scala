package org.github.bromel777.yaXMPPc.modules.servers

import java.net.InetSocketAddress
import java.util.UUID

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift}
import fs2.Stream
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPServerSettings
import org.github.bromel777.yaXMPPc.domain.stanza.Stanza
import org.github.bromel777.yaXMPPc.modules.ConnectedClient
import tofu.logging.Logging
import tofu.syntax.monadic._

/**
  * Server, which establish TCP
  */

final class TCPServer[F[_]: Concurrent: ContextShift: Logging](
  serverSettings: XMPPServerSettings,
  socketGroup: SocketGroup,
  clients: Ref[F, Map[String, Boolean]]
) extends Server[F, Stanza, Stanza] {

  override def receiverStream: Stream[F, Stanza] =
    socketGroup.server[F](new InetSocketAddress(serverSettings.port)).map { clientSocketResource =>
      Stream
        .resource(clientSocketResource)
        .flatMap { clientSocket =>
          Stream
            .bracket(ConnectedClient[F](clientSocket).flatTap(setUserOnline))(
              setOfflineStatus
            )
            .flatMap(handleClient)
        }
    }.parJoinUnbounded

  override def send(out: Stanza, user: UUID): F[Unit] = ???

  private def setOfflineStatus(connectedClient: ConnectedClient[F]): F[Unit] = ???

  private def setUserOnline(connectedClient: ConnectedClient[F]): F[Unit] = ???

  private def handleClient(client: ConnectedClient[F]): Stream[F, Stanza] = ???
}

object TCPServer {

  def make[F[_]: Concurrent: ContextShift: Logging](serverSettings: XMPPServerSettings, socketGroup: SocketGroup): F[TCPServer[F]] =
    Ref.of[F, Map[String, Boolean]](Map.empty[String, Boolean]).map { userMap =>
      new TCPServer[F](serverSettings, socketGroup, userMap)
    }
}
