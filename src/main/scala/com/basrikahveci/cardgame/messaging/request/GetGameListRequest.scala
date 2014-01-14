package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.{Lobby, Session}
import cardgame.domain.User
import cardgame.domain.game.GameKey

class GetGameListRequest(gameId: Long, score: Int, val next: Boolean) extends Request with Lobby {

  val key: GameKey = new GameKey(gameId, score)

  def handle(session: Session, user: User) = sendGameList(user, key, next)

}
