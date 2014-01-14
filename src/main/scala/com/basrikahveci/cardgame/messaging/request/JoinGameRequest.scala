package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.{Lobby, Session}
import cardgame.domain.User

class JoinGameRequest(val gameId: Long) extends Request with Lobby {

  def handle(session: Session, user: User) = joinGame(user, gameId)

}
