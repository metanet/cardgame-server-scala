package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Success, Response}
import cardgame.domain.NexTurn

class GamePlayNotification(val nextTurn: NexTurn, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
