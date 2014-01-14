package com.basrikahveci
package cardgame.domain

import cardgame.core._
import scala.Option
import actors.Actor
import com.weiglewilczek.slf4s.Logger
import cardgame.domain.commands._
import cardgame.core.db.QueryEvaluatorContainer
import java.sql.Timestamp
import cardgame.messaging.response.{PlayerInfo, GetUserProfileResponse, CreateGameResponse}
import cardgame.messaging.{Response, Success}
import cardgame.domain.game.GameSettings
import cardgame.domain.commands.AcceptInvitationOf
import cardgame.domain.commands.AddSessionPoints
import scala.Some
import cardgame.domain.commands.Play
import cardgame.domain.commands.CreateGameWith
import cardgame.domain.commands.JoinGame
import cardgame.domain.commands.InvitedBy
import cardgame.domain.commands.SetStatus
import cardgame.domain.commands.SendProfile
import cardgame.domain.commands.SetSettings
import cardgame.domain.commands.EndGame

trait UserStatus {
  def ordinal: Int
}

object Offline extends UserStatus {
  def ordinal = 0
}

object Online extends UserStatus {
  def ordinal = 1
}

object Waiting extends UserStatus {
  def ordinal = 2
}

object Playing extends UserStatus {
  def ordinal = 3
}

package commands {

import cardgame.domain.game.GameSettings

case class SetStatus(status: UserStatus)

case object OneMoreWin

case object OneMoreLose

case object OneMoreLeave

case class AddSessionPoints(pointsToAdd: Int)

case class CreateGame(multiPlayer: Boolean)

case class EndGame(bySystem: Boolean)

case class JoinGame(game: Game)

case object LeaveGame

case class Play(index: Int)

case object StartGame

case object SetReady

case class SetSettings(settings: GameSettings)

case class SendProfile(sendTo: User)

case object PlayNow

case class Invite(invitee: User)

case class InvitedBy(inviter: Player)

case class AcceptInvitationOf(inviter: User)

case class CreateGameWith(other: User)

case class Say(text: String)

}


class UserIdentity(val userId: Long, val firstName: String, var status: Int) {
  override def toString = "UserIdentity{userId=" + userId + ",firstName=" + firstName + ",status=" + status + "}"
}

class UserProfile(val identity: UserIdentity, val lastSignInTime: Long, val points: Int, val wins: Int, val loses: Int, val leaves: Int, val gameId: Option[Long])


object User {
  val logger = Logger(classOf[User])
}

class User(val id: Long, val firstName: String, val friends: Array[Long], val session: Session, private val _points: Int, private val _wins: Int, private val _loses: Int, private val _leaves: Int) extends Actor with Player with OnlineUsers with Lobby with Invitations {

  val identity = new UserIdentity(id, firstName, Online.ordinal)

  private var game: Option[Game] = None

  @volatile private var _sessionPoints = 0

  private var sessionWins = 0

  private var sessionLoses = 0

  private var sessionLeaves = 0

  start

  def act {
    loop {
      react {
        case SetStatus(status) =>
          _setStatus(status)
        case OneMoreWin =>
          _oneMoreWin
        case OneMoreLose =>
          _oneMoreLose
        case OneMoreLeave =>
          _oneMoreLeave
        case AddSessionPoints(points) =>
          _addSessionPoints(points)
        case CreateGame(multiPlayer) =>
          _create(multiPlayer)
        case EndGame(bySystem) =>
          _endGame(bySystem)
        case JoinGame(game) =>
          _join(game)
        case LeaveGame =>
          _leaveGame
        case Play(index) =>
          _play(index)
        case SetReady =>
          _setReady
        case SetSettings(settings) =>
          _setGameSettings(settings)
        case StartGame =>
          _startGame
        case SendProfile(sendTo) =>
          _sendProfile(sendTo)
        case PlayNow =>
          _playNow
        case Invite(invitee) =>
          _invite(invitee)
        case InvitedBy(inviter) =>
          _getInvitedBy(inviter)
        case AcceptInvitationOf(inviter) =>
          _acceptInvitationOf(inviter)
        case CreateGameWith(other) =>
          _createGameWith(other)
        case Say(text) =>
          _say(text)
        case any: Any =>
          User.logger warn "Invalid message to User " + id + " , Message: " + any
      }
    }
  }

  def isInGame = game.isDefined

  def points = _points + _sessionPoints

  private def wins = _wins + sessionWins

  private def loses = _loses + sessionLoses

  private def leaves = _leaves + sessionLeaves

  def setStatus(status: UserStatus) = this ! SetStatus(status)

  private def _setStatus(status: UserStatus) {
    identity.status = status.ordinal
    notifyFriendsOf(this)
  }

  def oneMoreWin = this ! OneMoreWin

  private def _oneMoreWin = sessionWins += 1

  def oneMoreLose = this ! OneMoreLose

  private def _oneMoreLose = sessionLoses += 1

  def oneMoreLeave = this ! OneMoreLeave

  private def _oneMoreLeave = sessionLeaves += 1

  def addSessionPoints(pointsToAdd: Int) = this ! AddSessionPoints(pointsToAdd)

  private def _addSessionPoints(pointsToAdd: Int) = _sessionPoints += pointsToAdd

  def create(multiPlayer: Boolean) = this ! CreateGame(multiPlayer)

  def _create(multiPlayer: Boolean) = game match {
    case Some(game) =>
      User.logger warn "[create-game-while-in-game] User Id: " + id
    case None =>
      val game = new Game(this, multiPlayer)
      _own(game)
      sendMessage(new CreateGameResponse(true, Success.ordinal, new PlayerInfo(identity, Game.OwnerPosition, 0)))

      if (!multiPlayer) {
        val bot = new Bot(game)
        game.joinedBy(bot)
        game.setReady(bot)
      }
  }

  private def _own(game: Game) {
    this.game = Some(game)
    setStatus(Waiting)
  }

  def join(game: Game) = this ! JoinGame(game)

  private def _join(game: Game) {
    this.game = Some(game)
    setStatus(Playing)
  }

  def leaveGame = this ! LeaveGame

  def _leaveGame {
    game = None
    setStatus(Online)
  }

  def endGame(bySystem: Boolean) = this ! EndGame(bySystem)

  private def _endGame(bySystem: Boolean) = game.foreach(_.leftBy(this, bySystem))

  def signOut(signOutTime: Long, bySystem: Boolean) {
    _endGame(bySystem)

    val signInTime = session.signInTime
    QueryEvaluatorContainer.queryEvaluator.execute("UPDATE users SET last_signed_in = ?, points = points + ?, wins = wins + ?, loses = loses + ?, leaves = leaves + ? WHERE id = ?", new Timestamp(signInTime), _sessionPoints, sessionWins, sessionLoses, sessionLeaves, id)
    QueryEvaluatorContainer.queryEvaluator.execute("INSERT INTO session_logs(user_id, sign_in_time, sign_out_time, insert_time) VALUES(?, ?, ?, ?) ", id, new Timestamp(signInTime), new Timestamp(signOutTime), new Timestamp(signOutTime))

    setStatus(Offline)
  }

  def play(index: Int) = this ! Play(index)

  private def _play(index: Int) = game.foreach(_.playedBy(this, index))

  def startGame = this ! StartGame

  private def _startGame = game.foreach(_.startedBy(this))

  def setReady = this ! SetReady

  private def _setReady = game.foreach(_.setReady(this))

  def setGameSettings(settings: GameSettings) = this ! SetSettings(settings)

  private def _setGameSettings(settings: GameSettings) = game.foreach(_.setGameSettings(this, settings))

  def sendProfile(sendTo: User) = this ! SendProfile(sendTo)

  private def _sendProfile(sendTo: User) = sendTo.sendMessage(new GetUserProfileResponse(id, new UserProfile(identity, session.signInTime, points, wins, loses, leaves, (if (game.isDefined) Some(id) else None))))

  def playNow = this ! PlayNow

  private def _playNow = game match {
    case Some(game) =>
      User.logger warn "[play-now-while-already-in-game] User Id: " + id
    case None =>
      playNow(this)
  }

  def invite(invitee: User) = this ! Invite(invitee)

  private def _invite(invited: User) = game match {
    case Some(game) =>
      game invite invited
    case None =>
      invited getInvitedBy this
  }

  def getInvitedBy(inviter: Player) = this ! InvitedBy(inviter)

  private def _getInvitedBy(inviter: Player) = game match {
    case Some(game) =>
      game getInvitedBy inviter
    case None =>
      addInvitation(inviter, this)
  }

  def acceptInvitationOf(inviter: User) = this ! AcceptInvitationOf(inviter)

  private def _acceptInvitationOf(inviter: User) {
    game match {
      case Some(game) =>
        User.logger warn "[can-not-accept-invt.-while-in-game] User Id: " + id + " , Inviter Id: " + inviter.id
      case None =>
        inviter createGameWith this
    }
  }

  def createGameWith(other: User) = this ! CreateGameWith(other)

  private def _createGameWith(other: User) {
    _create(true)
    game.foreach(_.joinedBy(other))
  }

  def say(text: String) = this ! Say(text)

  private def _say(text: String) = game.foreach(_.chat(this, text))

  def sendMessage(response: Response) = session.sendMessage(response)

  override def hashCode() = id.hashCode()

  override def equals(other: Any) = other match {
    case u: User => id == u.id
    case _ => false
  }

  override def toString = "User{" + identity.toString + "}"

  override def exceptionHandler = {
    case e: Exception =>
      User.logger.error("[exception-in-user] UserId: " + id, e)
  }
}
