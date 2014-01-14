package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.Session
import cardgame.domain.User
import cardgame.domain.game.GameSettings

class SetGameSettingsRequest(val gameSettings: GameSettings) extends Request {

  def handle(session: Session, user: User) = user setGameSettings gameSettings

}
