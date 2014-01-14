package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.Session
import cardgame.domain.User

class GameChatRequest(val text: String) extends Request {
  def handle(session: Session, user: User) {
    user.say(text)
  }
}
