package com.basrikahveci
package cardgame.messaging.response

import cardgame.domain.UserIdentity
import cardgame.messaging.{Success, Response}

class InviteUserNotification(val inviter: UserIdentity, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
