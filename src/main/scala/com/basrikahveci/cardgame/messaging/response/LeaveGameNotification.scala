package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Success, Response}

class LeaveGameNotification(val kickedBySystem: Boolean, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
