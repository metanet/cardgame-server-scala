package com.basrikahveci
package cardgame.core

import cardgame.domain.Player
import actors.Actor
import compat.Platform
import cardgame.messaging.response.{InviteUserNotification, InviteUserResponse}
import cardgame.messaging.{Success, AlreadyInvited}
import com.weiglewilczek.slf4s.Logger


trait Invitations {

  def addInvitation(inviter: Player, invited: Player) = ActiveInvitations.Instance ! AddInvitation(inviter, invited)

  def removeInvitation(inviter: Player, invited: Player) = ActiveInvitations.Instance ! RemoveInvitation(inviter, invited)

}

object ActiveInvitations {
  val Instance = new ActiveInvitations
}

case class AddInvitation(inviter: Player, invited: Player)

case class RemoveInvitation(inviter: Player, invited: Player)

case class Invitation(val inviter: Long, val invitee: Long)(var invitationTime: Long)


class ActiveInvitations extends Actor {

  val logger = Logger(classOf[ActiveInvitations])

  val invitations = scala.collection.mutable.Set[Invitation]()

  start

  def act {
    loop {
      react {
        case AddInvitation(inviter, invited) =>
          _add(inviter, invited)
        case RemoveInvitation(inviter, invited) =>
          _remove(inviter, invited)
      }
    }
  }

  private def _add(inviter: Player, invited: Player) {
    val now: Long = Platform.currentTime
    val newInvitation = Invitation(inviter.id, invited.id)(now)

    invitations.find(_ == newInvitation) match {
      case Some(oldInvitation) =>
        if (oldInvitation.invitationTime + 60000 > now) {
          inviter sendMessage new InviteUserResponse(false, AlreadyInvited.ordinal)
        } else {
          oldInvitation.invitationTime = now
          invited sendMessage new InviteUserNotification(inviter.identity)
          inviter sendMessage new InviteUserResponse(true, Success.ordinal)
        }
      case None =>
        invitations += newInvitation
        invited sendMessage new InviteUserNotification(inviter.identity)
        inviter sendMessage new InviteUserResponse(true, Success.ordinal)
    }
  }

  private def _remove(inviter: Player, invited: Player) {
    invitations -= new Invitation(inviter.id, invited.id)(Platform.currentTime)
    invitations -= new Invitation(invited.id, inviter.id)(Platform.currentTime)
  }

  override def exceptionHandler = {
    case e: Exception =>
      logger.error("[exception-in-invitations]", e)
  }
}
