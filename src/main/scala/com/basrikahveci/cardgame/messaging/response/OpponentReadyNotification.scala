package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Success, Response}

class OpponentReadyNotification(val success: Boolean = true, val reason: Int = Success.ordinal) extends Response {

}
