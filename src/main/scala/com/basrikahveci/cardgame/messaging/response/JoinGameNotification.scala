package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Response, Success}

class JoinGameNotification(val opponent: PlayerInfo, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
