package com.basrikahveci
package cardgame.messaging

import cardgame.core.Session
import cardgame.domain.User

trait Request {

  def handle(session: Session, user: User): Unit

}
