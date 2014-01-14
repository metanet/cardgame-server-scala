package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Success, Response}
import cardgame.domain.GameRound

class StartGameNotification(val round: GameRound, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
