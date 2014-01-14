package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Fail, Response}
import cardgame.domain.UserIdentity

object CreateGameResponse {
  val FAIL = new CreateGameResponse(false, Fail.ordinal, null)
}

class PlayerInfo(val user: UserIdentity, val position: Int, val score: Int)

class CreateGameResponse(val success: Boolean, val reason: Int, val owner: PlayerInfo) extends Response
