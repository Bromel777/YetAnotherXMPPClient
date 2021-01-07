package org.github.bromel777.yaXMPPc.programs.ca

import org.github.bromel777.yaXMPPc.programs.Program
import fs2.Stream

final class Server[F[_]]() extends Program[F] {

  override def run: Stream[F, Unit] = ???
}
