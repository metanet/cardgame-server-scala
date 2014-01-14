package com.basrikahveci
package cardgame.messaging.response

import cardgame.messaging.Response

class InviteUserResponse(val success: Boolean, val reason: Int) extends Response
