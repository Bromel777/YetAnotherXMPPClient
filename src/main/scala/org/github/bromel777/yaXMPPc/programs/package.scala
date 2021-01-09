package org.github.bromel777.yaXMPPc

import cats.Functor
import cats.effect.{Concurrent, ContextShift, Sync, Timer}
import fs2.io.tcp.SocketGroup
import org.github.bromel777.yaXMPPc.context.{AppContext, HasAppContext}
import org.github.bromel777.yaXMPPc.errors.Err
import org.github.bromel777.yaXMPPc.services.Cryptography
import tofu.Raise.ContravariantRaise
import tofu.logging.Logs
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.embed._

package object programs {

  def getProgram[F[_]: HasAppContext: Concurrent: ContextShift: ContravariantRaise[*[_], Err]: Timer](
    progName: String,
    socketGroup: SocketGroup,
    cryptography: Cryptography[F]
  )(implicit logs: Logs[F, F]): Program[F] =
    (tofu.syntax.context.context[F] flatMap { (ctx: AppContext) =>
      progName match {
        case "xmppClient" if ctx.XMPPClientSettings.isDefined =>
          xmpp.XMPPClientProgram.make[F](ctx.XMPPClientSettings.get, socketGroup, cryptography)
        case "xmppClient" =>
          Err("Impossible to run XMPP Client with empty settings!").raise[F, Program[F]]
        case "xmppServer" if ctx.XMPPServerSettings.isDefined =>
          xmpp.XMPPServerProgram.make[F](ctx.XMPPServerSettings.get, socketGroup)
        case "xmppServer" =>
          Err("Impossible to run XMPP Server with empty settings!").raise[F, Program[F]]
        case "caServer" if ctx.caSettings.isDefined =>
          ca.CAServerProgram.make[F](ctx.caSettings.get)
        case "caServer" =>
          Err("Impossible to run CA Server with empty settings!").raise[F, Program[F]]
        case _ =>
          Err("Unknown type of program!").raise[F, Program[F]]
      }
    }).embed
}
