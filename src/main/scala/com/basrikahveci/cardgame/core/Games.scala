package com.basrikahveci
package cardgame.core

import com.weiglewilczek.slf4s.Logger
import cardgame.domain.Game
import util.Random
import collection.mutable.{ArrayBuffer, Map}
import cardgame.domain.game.{GameInfo, GameKey}
import java.util
import util.Comparator

object Games {
  val Instance = new Games
  val GamesPerPage = 5
}

object GameKeysAscending extends Comparator[GameKey] {
  def compare(x: GameKey, y: GameKey) = {
    val result = x.score.compare(y.score)
    if (result != 0) result else x.gameId.compare(y.gameId)
  }
}

object GameKeysDescending extends Comparator[GameKey] {
  def compare(x: GameKey, y: GameKey) = {
    val result = y.score.compare(x.score)
    if (result != 0) result else y.gameId.compare(x.gameId)
  }
}

class Games {
  private val logger = Logger(classOf[Games])

  private val waitingGamesById = Map[Long, Game]()

  private val playingGamesById = Map[Long, Game]()

  private val waitingGamesAscending = new util.TreeMap[GameKey, GameInfo](GameKeysAscending)

  private val waitingGamesDescending = new util.TreeMap[GameKey, GameInfo](GameKeysDescending)

  private val _random = new Random

  def addWaiting(game: Game) {
    if (!waitingGamesById.contains(game.id)) {
      waitingGamesById += ((game.id, game))
      waitingGamesAscending.put(game.key, game.info)
      waitingGamesDescending.put(game.key, game.info)
    } else {
      logger warn "[add-waiting-failed] Game of user " + game.key.gameId + " is already in waiting games."
    }

    _logInternalState
  }

  def getWaiting(gameId: Long) = waitingGamesById.get(gameId)

  def removeWaiting(gameId: Long) {
    waitingGamesById.remove(gameId) match {
      case Some(game) =>
        waitingGamesAscending remove game.key
        waitingGamesDescending remove game.key
      case None =>
        logger warn "[remove-waiting-failed] Game with id " + gameId + " is not in waiting games."
    }

    _logInternalState
  }

  def removePlaying(gameId: Long) {
    playingGamesById -= gameId
    _logInternalState
  }

  def moveToWaiting(gameId: Long) {
    playingGamesById.remove(gameId) match {
      case Some(game) =>
        addWaiting(game)
      case None =>
        logger warn "[move-to-waiting-failed] Game with id " + gameId + " is not in playing games."
    }

    _logInternalState
  }

  def moveToPlaying(gameId: Long) {
    waitingGamesById.remove(gameId) match {
      case Some(game) =>
        playingGamesById += ((game.id, game))
        waitingGamesAscending.remove(game.key)
        waitingGamesDescending.remove(game.key)
      case None =>
        logger warn "[move-to-waiting-failed] Game with id " + gameId + " is not in playing games."
    }

    _logInternalState
  }

  def nextWaitingGames(key: GameKey) = {
    val matchingGames = new ArrayBuffer[GameInfo]()
    val candidateGames = waitingGamesAscending.tailMap(key)
    val gameCountToTake = if (candidateGames.size > Games.GamesPerPage) Games.GamesPerPage else candidateGames.size
    val candidateGamesIterator = candidateGames.entrySet.iterator

    for (_ <- 1 to gameCountToTake) {
      matchingGames += candidateGamesIterator.next.getValue
    }

    val remainingCount = Games.GamesPerPage - gameCountToTake
    if (remainingCount > 0) {
      val prevGames = new ArrayBuffer[GameInfo]()
      val prevKey = if (matchingGames.size == 0) key else matchingGames(0).key
      val candidatePrevGames = waitingGamesDescending.tailMap(prevKey, false)
      val prevMax = if (candidatePrevGames.size > remainingCount) remainingCount else candidatePrevGames.size
      val candidatePrevGamesIterator = candidatePrevGames.entrySet.iterator
      for (_ <- 1 to prevMax) {
        prevGames += candidatePrevGamesIterator.next.getValue
      }

      prevGames.reverse ++ matchingGames
    } else {
      matchingGames
    }
  }

  def previousWaitingGames(key: GameKey) = {
    val matchingGames = new ArrayBuffer[GameInfo]()
    val candidateGames = waitingGamesDescending.tailMap(key)
    val gameCountToTake = if (candidateGames.size > Games.GamesPerPage) Games.GamesPerPage else candidateGames.size
    val candidateGamesIterator = candidateGames.entrySet.iterator

    for (_ <- 1 to gameCountToTake) {
      matchingGames += candidateGamesIterator.next.getValue
    }

    val remainingCount = Games.GamesPerPage - gameCountToTake
    if (remainingCount > 0) {
      val nextGames = new ArrayBuffer[GameInfo]()
      val nextKey = if (matchingGames.size == 0) key else matchingGames(0).key
      val candidatePrevGames = waitingGamesAscending.tailMap(nextKey, false)
      val prevMax = if (candidatePrevGames.size > remainingCount) remainingCount else candidatePrevGames.size
      val candidatePrevGamesIterator = candidatePrevGames.entrySet.iterator
      for (_ <- 1 to prevMax) {
        nextGames += candidatePrevGamesIterator.next.getValue
      }

      matchingGames ++ nextGames
    } else {
      matchingGames
    }
  }

  def numberOfWaitingGames = waitingGamesById.size

  def random = {
    var game: Option[Game] = None

    if (waitingGamesById.size == 1) {
      game = waitingGamesById.get(waitingGamesAscending.firstKey.gameId)
    } else if (waitingGamesById.size > 1) {
      val minKey = waitingGamesAscending.firstKey
      val maxKey = waitingGamesAscending.lastKey

      val randomScore = minKey.score + _random.nextInt(maxKey.score - minKey.score + 1)
      val up = randomScore % 2 == 0
      val randomKey = new GameKey(0, randomScore)

      val matchingKey = if (up) waitingGamesAscending.ceilingKey(randomKey) else waitingGamesAscending.lowerKey(randomKey)
      game = waitingGamesById.get(matchingKey.gameId)
    }

    game
  }

  private def _logInternalState {
    logger info "[games] Waiting Games: " + waitingGamesById
    logger info "[games] Playing Games: " + playingGamesById
    logger info "[games] Waiting Games Ascending: " + waitingGamesAscending
    logger info "[games] Waiting Games Descending: " + waitingGamesDescending
  }
}
