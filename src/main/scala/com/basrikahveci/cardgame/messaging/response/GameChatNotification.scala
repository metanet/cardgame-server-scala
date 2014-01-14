package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Response, Success}

class GameChatNotification(val text: String, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
