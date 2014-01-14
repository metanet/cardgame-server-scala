package com.basrikahveci
package cardgame.core

import cardgame.domain.{Game, User}
import cardgame.messaging._
import actors.Actor
import com.weiglewilczek.slf4s.Logger
import cardgame.core.commands._
import response.{GetGameListResponse, JoinGameResponse}
import cardgame.core.commands.RemovePlaying
import cardgame.core.commands.MoveToWaiting
import cardgame.core.commands.JoinGame
import cardgame.core.commands.SendGameList
import cardgame.domain.game.GameKey
import cardgame.core.commands.RemoveWaiting

object DefaultGameManager {
  val Instance = new DefaultGameManager
}

package commands {

case class AddWaiting(game: Game)

case class RemovePlaying(gameId: Long)

case class MoveToWaiting(gameId: Long)

case class RemoveWaiting(gameId: Long)

case class MoveToPlaying(gameId: Long)

case class SendGameList(user: User, key: GameKey, next: Boolean)

case class JoinGame(user: User, gameId: Long)

case class PlayNow(user: User)

case class ChangeCCU(increase: Boolean)

}

trait Lobby {

  def sendGameList(user: User, key: GameKey, next: Boolean) = DefaultGameManager.Instance ! SendGameList(user, key, next)

  def joinGame(user: User, gameId: Long) = DefaultGameManager.Instance ! JoinGame(user, gameId)

  def removePlaying(gameId: Long) = DefaultGameManager.Instance ! RemovePlaying(gameId)

  def moveToWaiting(gameId: Long) = DefaultGameManager.Instance ! MoveToWaiting(gameId)

  def removeWaiting(gameId: Long) = DefaultGameManager.Instance ! RemoveWaiting(gameId)

  def moveToPlaying(gameId: Long) = DefaultGameManager.Instance ! MoveToPlaying(gameId)

  def addWaiting(game: Game) = DefaultGameManager.Instance ! AddWaiting(game)

  def playNow(user: User) = DefaultGameManager.Instance ! PlayNow(user)

  def increaseCCU = DefaultGameManager.Instance ! ChangeCCU(true)

  def decreaseCCU = DefaultGameManager.Instance ! ChangeCCU(false)

}

class DefaultGameManager extends Actor with OnlineUsers {

  val logger = Logger(classOf[DefaultGameManager])

  var nextGameId: Int = 1

  val games = Games.Instance

  var ccu = 0


  start

  def act {
    loop {
      react {
        case SendGameList(user, key, next) =>
          _sendGameList(user, key, next)
        case AddWaiting(game) =>
          _addWaiting(game)
        case JoinGame(user, gameId) =>
          _joinGame(user, gameId)
        case RemovePlaying(gameId) =>
          games.removePlaying(gameId)
        case MoveToWaiting(gameId) =>
          games.moveToWaiting(gameId)
        case RemoveWaiting(gameId) =>
          games.removeWaiting(gameId)
        case MoveToPlaying(gameId) =>
          games.moveToPlaying(gameId)
        case PlayNow(user) =>
          _playNow(user)
        case ChangeCCU(increase) =>
          ccu += (if (increase) 1 else -1)
        case msg: Any =>
          logger warn "[invalid-game-manager-msg] Msg: " + msg
      }
    }
  }

  private def _addWaiting(game: Game) {
    games.addWaiting(game)
  }

  private def _sendGameList(user: User, key: GameKey, next: Boolean) {
    if (user.isInGame) {
      logger warn "[glr-from-in-game-user] User Id: " + user.id
    }

    val matchingGames = if (next) games.nextWaitingGames(key) else games.previousWaitingGames(key)

    user sendMessage new GetGameListResponse(matchingGames, games.numberOfWaitingGames, ccu)
  }

  private def _joinGame(user: User, gameId: Long) {
    user.endGame(false)

    games.getWaiting(gameId) match {
      case Some(game) =>
        game.joinedBy(user)
      case None =>
        user sendMessage new JoinGameResponse(null, null, false, GameNotFound.ordinal)
    }
  }

  private def _playNow(user: User) = games.random.foreach(_.joinedBy(user))

  override def exceptionHandler = {
    case e: Exception =>
      logger.error("[exception-in-default-game-manager]", e)
  }

}
