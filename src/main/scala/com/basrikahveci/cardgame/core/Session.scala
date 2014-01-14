package com.basrikahveci
package cardgame.core

import cardgame.domain.User
import actors.Actor
import compat.Platform
import org.jboss.netty.channel.Channel
import com.weiglewilczek.slf4s.Logger
import cardgame.messaging.{MessageFactory, Response}

case class SignInAs(user: User)

case class SendResponse(response: Response)

case class CloseSession(bySystem: Boolean, silent: Boolean)

object Session {
  val logger = Logger(classOf[Session])
}

class Session(val channel: Channel) extends Actor with OnlineUsers {

  @volatile
  private var signedInUser: User = null

  private var signInTimeInMillis: Long = -1

  // Actor starts
  start


  def act() {
    while (true) {
      receive {
        case SignInAs(user) =>
          _signInAs(user)
        case SendResponse(response) =>
          _sendMessage(response)
        case CloseSession(bySystem, silent) =>
          _signOut(bySystem, silent)
          _closeChannel
          exit
        case invalid: Any =>
          Session.logger.error("[invalid-msg-to-session-actor] Message: " + invalid)
      }
    }
  }

  def user: User = signedInUser

  def signInTime: Long = signInTimeInMillis

  def isUserSignedIn = signedInUser != null

  def signInAs(user: User) = this ! SignInAs(user)

  def close(bySystem: Boolean, silent: Boolean = false) = this ! CloseSession(bySystem, silent)

  def sendMessage(response: Response) = this ! SendResponse(response)

  private def _signInAs(user: User) {
    signedInUser = user
    signInTimeInMillis = Platform.currentTime
  }

  private def _signOut(bySystem: Boolean, silent: Boolean) {
    if (signedInUser != null) {
      if (!silent) {
        remove(signedInUser)
      }
      signedInUser.signOut(Platform.currentTime, bySystem)
      signedInUser = null
    }
  }

  def _closeChannel: Unit = channel.close

  private def _sendMessage(response: Response) {
    if (channel.isOpen) {
      val encoded = MessageFactory.Instance.encode(response)
      channel.write(encoded)
      Session.logger.info("[message-sent] User Id: " + (if (signedInUser != null) signedInUser.id else "-") + " , Response: " + encoded)
    }

  }

}
