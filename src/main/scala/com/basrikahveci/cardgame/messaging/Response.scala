package com.basrikahveci
package cardgame.messaging

trait Reason {
  val ordinal: Int
  val success: Boolean
}

case object Success extends Reason {
  val ordinal = 0
  val success = true
}

case object Fail extends Reason {
  val ordinal = 1
  val success = false
}

case object GameNotFound extends Reason {
  val ordinal = 2
  val success = false
}

case object GameFull extends Reason {
  val ordinal = 3
  val success = false
}

case object InviterGameFull extends Reason {
  val ordinal = 4
  val success = false
}

case object InvitedUserNotOnline extends Reason {
  val ordinal = 5
  val success = false
}

case object InvitedUserInGame extends Reason {
  val ordinal = 6
  val success = false
}

case object AlreadyInvited extends Reason {
  val ordinal = 7
  val success = false
}

case object CanNotInviteHimself extends Reason {
  val ordinal = 8
  val success = false
}

case object NotInGame extends Reason {
  val ordinal = 9
  val success = false
}

case object NotTurn extends Reason {
  val ordinal = 10
  val success = false
}

case object CardNotPlayable extends Reason {
  val ordinal = 11
  val success = false
}

trait Response {
  val success: Boolean
  val reason: Int
}
