package org.github.bromel777.yaXMPPc

import cats.Functor
import cats.effect.Sync
import org.github.bromel777.yaXMPPc.context.{AppContext, HasAppContext}
import org.github.bromel777.yaXMPPc.errors.Err
import tofu.Raise.ContravariantRaise
import tofu.logging.Logs
import tofu.syntax.monadic._
import tofu.syntax.raise._
import tofu.syntax.embed._

package object programs {

  def getProgram[F[_]: HasAppContext: Sync: ContravariantRaise[*[_], Err]](
    progName: String
  )(implicit logs: Logs[F, F]): Program[F] =
    (tofu.syntax.context.context[F] flatMap { (ctx: AppContext) =>
      progName match {
        case "xmppClient" if ctx.XMPPSettings.isDefined =>
          xmpp.XMPPClientProgram.make[F](ctx.XMPPSettings.get)
        case "xmppClient" =>
          Err("Impossible to run XMPP Client with empty settings!").raise[F, Program[F]]
        case "xmppServer" if ctx.XMPPSettings.isDefined =>
          xmpp.XMPPServerProgram.make[F](ctx.XMPPSettings.get)
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
