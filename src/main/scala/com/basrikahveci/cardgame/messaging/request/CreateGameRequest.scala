package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.Session
import cardgame.domain.User
import com.weiglewilczek.slf4s.Logger

object CreateGameRequest {
  val logger = Logger(classOf[CreateGameRequest])
}

class CreateGameRequest(val multiPlayer: Boolean) extends Request {

  def handle(session: Session, user: User) = user.create(multiPlayer)

}
