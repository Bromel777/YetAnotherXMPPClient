package org.github.bromel777.yaXMPPc.programs.xmpp

import org.github.bromel777.yaXMPPc.programs.Program
import fs2.Stream

final class Client[F[_]]() extends Program[F] {

  override def run: Stream[F, Unit] = ???
}
