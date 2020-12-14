package org.github.bromel777.yaXMPPc.domain

import io.estatico.newtype.macros.newtype

package object stanza {

  @newtype final case class Sender(value: String)

  @newtype final case class Receiver(value: String)
}
