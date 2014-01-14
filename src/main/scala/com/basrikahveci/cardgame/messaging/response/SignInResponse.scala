package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.{Success, Response}
import cardgame.domain.UserIdentity

class SignInResponse(val userId: Long, val firstName: String, val signInTime: Long, val points: Int, val onlineFriends: Seq[UserIdentity], val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
