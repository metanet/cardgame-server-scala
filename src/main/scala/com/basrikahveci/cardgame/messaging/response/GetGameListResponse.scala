package com.basrikahveci
package cardgame.messaging.response

import collection.mutable.ArrayBuffer
import cardgame.domain.game.GameInfo
import cardgame.messaging.{Response, Success}

class GetGameListResponse(val matchingGames: ArrayBuffer[GameInfo], val numberOfWaitingGames: Int, val numberOfOnlineUsers: Int, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
