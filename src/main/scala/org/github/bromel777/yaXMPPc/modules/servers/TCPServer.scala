package org.github.bromel777.yaXMPPc.modules.servers

import java.net.InetSocketAddress
import java.util.UUID

import cats.effect.concurrent.Ref
import cats.effect.{Concurrent, ContextShift}
import cats.syntax.traverse._
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.configs.XMPPServerSettings
import org.github.bromel777.yaXMPPc.domain.xmpp.stanza.Stanza
import org.github.bromel777.yaXMPPc.modules.ConnectedClient
import tofu.logging.Logging
import tofu.syntax.monadic._
import tofu.syntax.logging._

import scala.collection.immutable.HashMap

/**
  * Server, which establish TCP
  */

final class TCPServer[F[_]: Concurrent: ContextShift: Logging](
  serverSettings: XMPPServerSettings,
  socketGroup: SocketGroup,
  clients: Ref[F, HashMap[UUID, ConnectedClient[F]]],
  infoQueue: Queue[F, TCPEvent]
) extends Server[F, (Stanza, UUID), Stanza] {

  override def receiverStream: Stream[F, (Stanza, UUID)] =
    socketGroup
      .server[F](new InetSocketAddress(serverSettings.port))
      .map { clientSocketResource =>
        Stream
          .resource(clientSocketResource)
          .flatMap { clientSocket =>
            Stream
              .bracket(ConnectedClient[F](clientSocket).flatTap(setUserOnline))(
                setOfflineStatus
              )
              .flatMap(handleClient)
          }
      }
      .parJoinUnbounded

  override def send(out: Stanza, user: UUID): F[Unit] =
    clients.get.flatMap(_.find(_._1 == user).traverse {
      case (_, client) => client.messageSocket.write1(Stanza.toXML(out))
    }.void)

  private def setOfflineStatus(connectedClient: ConnectedClient[F]): F[Unit] =
    info"Client with id ${connectedClient.id} disconnect" >> clients.update(_ - connectedClient.id)

  private def setUserOnline(connectedClient: ConnectedClient[F]): F[Unit] =
    info"New connection from user with uuid ${connectedClient.id}" >> clients.update(_ + (connectedClient.id -> connectedClient))

  private def handleClient(client: ConnectedClient[F]): Stream[F, (Stanza, UUID)] =
    client.messageSocket.read.map(Stanza.parseXML).unNone.map(stanza => stanza -> client.id)
}

object TCPServer {

  def make[F[_]: Concurrent: ContextShift: Logging](
    serverSettings: XMPPServerSettings,
    socketGroup: SocketGroup,
    tcpEventQueue: Queue[F, TCPEvent]
  ): F[TCPServer[F]] =
    Ref.of[F, HashMap[UUID, ConnectedClient[F]]](HashMap.empty[UUID, ConnectedClient[F]]).map { userMap =>
      new TCPServer[F](serverSettings, socketGroup, userMap, tcpEventQueue)
    }
}
