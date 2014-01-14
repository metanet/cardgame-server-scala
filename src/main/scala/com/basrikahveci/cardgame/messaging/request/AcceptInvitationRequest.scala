package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.{OnlineUsers, Session}
import cardgame.domain.User

class AcceptInvitationRequest(val inviterId: Long) extends Request with OnlineUsers {
  def handle(session: Session, user: User) {
    acceptInvitationOf(inviterId, user)
  }
}
