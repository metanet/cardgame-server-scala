package com.basrikahveci
package cardgame.core

import cardgame.messaging.response.{SignInResponse, UserNotification}
import compat.Platform
import actors.Actor
import com.weiglewilczek.slf4s.Logger
import cardgame.domain.User

case class Settle(user: User)

case class NotifyFriends(user: User)

case class Remove(user: User)

case class GetUserProfile(userId: Long, sendTo: User)

case class InviteFriend(user: User, invitedUserId: Long)

case class AcceptInvitationOf(inviterId: Long, invited: User)

object DefaultOnlineUsers {
  val Instance = new DefaultOnlineUsers
}

trait OnlineUsers {

  def settle(user: User) = DefaultOnlineUsers.Instance ! Settle(user)

  def notifyFriendsOf(user: User) = DefaultOnlineUsers.Instance ! NotifyFriends(user)

  def remove(user: User) = DefaultOnlineUsers.Instance ! Remove(user)

  def getUserProfile(userId: Long, sendTo: User) = DefaultOnlineUsers.Instance ! GetUserProfile(userId, sendTo)

  def inviteFriend(user: User, invitedUserId: Long) = DefaultOnlineUsers.Instance ! InviteFriend(user, invitedUserId)

  def acceptInvitationOf(inviterId: Long, invited: User) = DefaultOnlineUsers.Instance ! AcceptInvitationOf(inviterId, invited)

}

class DefaultOnlineUsers extends Actor with Lobby {

  val logger = Logger(classOf[DefaultOnlineUsers])

  private val users = scala.collection.mutable.Map[Long, User]()

  start

  def act {
    loop {
      react {
        case Settle(user) =>
          _settle(user)
        case NotifyFriends(user) =>
          _notifyFriends(user)
        case Remove(user) =>
          _remove(user)
        case GetUserProfile(userId, sendTo) =>
          _getUserProfile(userId, sendTo)
        case InviteFriend(user, invitedUserId) =>
          _inviteFriend(user, invitedUserId)
        case AcceptInvitationOf(inviterId, invited) =>
          _acceptInvitationOf(inviterId, invited)
        case any: Any =>
          logger warn "Invalid message for Users. Message: " + any
      }
    }
  }

  private def _settle(settlingUser: User) {
    users.get(settlingUser.id) match {
      case Some(alreadySettledUser) =>
        alreadySettledUser.session.close(false, true)
        users -= settlingUser.id
      case None =>
        increaseCCU
    }

    settlingUser.session signInAs settlingUser
    users(settlingUser.id) = settlingUser

    val onlineFriendIds = onlineFriendsOf(settlingUser)

    settlingUser.sendMessage(new SignInResponse(settlingUser.id, settlingUser.firstName, settlingUser.session.signInTime, settlingUser.points, onlineFriendIds.map(users(_).identity)))
    _notifyFriends(settlingUser, onlineFriendIds)

    _logInternalState
  }

  private def onlineFriendsOf(settlingUser: User): Array[Long] = {
    settlingUser.friends.filter(users.contains)
  }

  private def _notifyFriends(user: User) {
    _notifyFriends(user, onlineFriendsOf(user))
  }

  private def _notifyFriends(user: User, others: Array[Long]) {
    val notification = new UserNotification(user.identity, Platform.currentTime)
    others.foreach {
      eachOne => users(eachOne) sendMessage notification
    }
  }

  private def _remove(leavingUser: User) {
    users -= leavingUser.id
    decreaseCCU

    _logInternalState
  }

  private def _acceptInvitationOf(inviterId: Long, invited: User) = users.get(inviterId).foreach(invited.acceptInvitationOf(_))

  private def _getUserProfile(userId: Long, sendTo: User) = users.get(userId).foreach(_.sendProfile(sendTo))

  private def _inviteFriend(user: User, invitedUserId: Long) = users.get(invitedUserId).foreach(user invite _)

  private def _logInternalState = logger info "[online-users] Users: " + users

  override def exceptionHandler = {
    case e: Exception =>
      logger.error("[exception-in-online-users]", e)
  }

}
