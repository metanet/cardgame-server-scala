package com.basrikahveci
package cardgame.messaging.response

import com.basrikahveci.cardgame.messaging.Response

class JoinGameResponse(val owner: PlayerInfo, val opponent: PlayerInfo, val success: Boolean, val reason: Int) extends Response