package com.basrikahveci
package cardgame.messaging.request

import cardgame.messaging.Request
import cardgame.core.{OnlineUsers, Session}
import cardgame.domain.User
import cardgame.core.db.QueryEvaluatorContainer
import compat.Platform
import java.sql.Timestamp
import org.apache.commons.codec.digest.DigestUtils

object SignInRequest {
  val SECRET = "oyfarfara"
}

class PersistentUserData(val userId: Long, val points: Int, val wins: Int, val loses: Int, val leaves: Int)

class SignInRequest(val userId: Long, val name: String, val friends: Array[Long], val signature: String) extends Request with OnlineUsers {

  def handle(session: Session, user: User) {
    if (!session.isUserSignedIn) {
      val signature = DigestUtils.sha256Hex(name + SignInRequest.SECRET)
      if (signature == this.signature) {
        loadUser match {
          case Some(userData) =>
            signInWith(session, userData)
          case None =>
            val userData = new PersistentUserData(userId, 1000, 0, 0, 0)
            persist(userData)
            signInWith(session, userData)
        }

      } else {
        session close true
      }
    } else {
      session close true
    }
  }

  def loadUser = {
    var userDataOption: Option[PersistentUserData] = None

    QueryEvaluatorContainer.queryEvaluator.selectOne(" SELECT points, wins, loses, leaves FROM users WHERE id = ? ", userId) {
      row =>
        userDataOption = Some(new PersistentUserData(userId, row.getInt("points"), row.getInt("wins"), row.getInt("loses"), row.getInt("leaves")))
    }

    userDataOption
  }

  def persist(userData: PersistentUserData) = QueryEvaluatorContainer.queryEvaluator.insert(" INSERT into users(id, registration_time, points) VALUES(?, ?, ?) ", userData.userId, new Timestamp(Platform.currentTime), userData.points)

  def signInWith(session: Session, userData: PersistentUserData) = settle(new User(userId, name, friends, session, userData.points, userData.wins, userData.loses, userData.leaves))

}
