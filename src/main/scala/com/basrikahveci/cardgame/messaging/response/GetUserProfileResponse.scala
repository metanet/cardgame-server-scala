package com.basrikahveci
package cardgame.messaging.response

import cardgame.domain.UserProfile
import cardgame.messaging.{Success, Response}

class GetUserProfileResponse(val userId: Long, val profile: UserProfile, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
