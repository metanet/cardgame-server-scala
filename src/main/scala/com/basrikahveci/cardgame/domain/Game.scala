package com.basrikahveci
package cardgame.domain

import cardgame.domain.game._
import util.Random
import cardgame.messaging.response._
import cardgame.core.{Invitations, Lobby, OnlineUsers}
import actors.Actor
import com.weiglewilczek.slf4s.Logger
import cardgame.messaging.{InvitedUserInGame, InviterGameFull, Success, GameFull}
import cardgame.domain.game.GameKey
import cardgame.card.{DeckShuffler, Card}
import collection.mutable.ArrayBuffer


package game {

import cardgame.card.Card

case class GameKey(val gameId: Long, val score: Int) {
  override def toString = "GameKey{gameId=" + gameId + ",score:" + score + "}"
}

class GameInfo(val key: GameKey, val owner: UserIdentity) {
  override def toString = "GameInfo{key=" + key + ",owner=" + owner + "}"
}

trait DeckSize {
  val numberOfCards: Int;
  val cardsToWin: Int;
}

class SMALL extends DeckSize {
  val numberOfCards: Int = 10
  val cardsToWin: Int = 6

  override def toString = "SMALL"
}

class MEDIUM extends DeckSize {
  val numberOfCards: Int = 15
  val cardsToWin: Int = 8

  override def toString = "MEDIUM"
}

class LARGE extends DeckSize {
  val numberOfCards: Int = 20
  val cardsToWin: Int = 11

  override def toString = "LARGE"
}

class GameSettings(val deckSize: DeckSize)

trait GameCardStatus {
  val playable: Boolean
}

object CLOSED extends GameCardStatus {
  val playable = true
}

object TURN extends GameCardStatus {
  val playable = false
}

object OPEN extends GameCardStatus {
  val playable = false
}


class GameCard(card: Card, val index: Int) {

  val id = card.id

  val name = card.name

  private var status: GameCardStatus = CLOSED

  def playable = status.playable

  def matches(other: GameCard) = id == other.id

  def turn = status = TURN

  def open = status = OPEN

  def close = status = CLOSED
}

trait Play {
  def play(currentTurn: Int, previousCard: GameCard, playedCard: GameCard): PlayResult
}

object FirstPlay extends Play {
  def play(currentTurn: Int, previousCard: GameCard, playedCard: GameCard): PlayResult = {
    playedCard.turn
    new PlayResult(SecondPlay, currentTurn, Array[Int](playedCard.index), Array[Int](), playedCard, false)
  }
}

object SecondPlay extends Play {
  def play(currentTurn: Int, previousCard: GameCard, playedCard: GameCard): PlayResult = {
    if (previousCard.matches(playedCard)) {
      previousCard.open
      playedCard.open

      new PlayResult(FirstPlay, currentTurn, Array[Int](previousCard.index, playedCard.index), Array[Int](), null, true)
    } else {
      previousCard.close
      playedCard.close

      new PlayResult(FirstPlay, (currentTurn + 1) % Game.NumberOfPlayers, Array[Int](playedCard.index), Array[Int](previousCard.index, playedCard.index), null, false)
    }
  }
}


}

object Game {
  val InvalidPosition = -1
  val OwnerPosition = 0
  val ParticipantPosition = 1
  val DrawPosition = 2
  val NumberOfPlayers = 2

  val logger = Logger(classOf[Game])
}

case class LeftBy(user: User, bySystem: Boolean)

case class JoinedBy(user: Player)

case class SetGameSettings(user: User, settings: GameSettings)

case class SetParticipantReady(participant: Player)

case class StartRound(cards: Array[Card])

case class StartBy(user: User)

case class PlayBy(user: Player, index: Int)

case class Invite(invited: User)

case class GetInvitedBy(inviter: Player)

case class Chat(player: User, message: String)

class GameRound(val currentTurn: Int, val cards: Seq[String])

class PlayResult(val next: Play, val nextTurn: Int, val openCards: Array[Int], val closeCards: Array[Int], val previousCard: GameCard, val matched: Boolean)

class NexTurn(val nextTurnPosition: Int, val previousTurnPosition: Int, val matched: Boolean, val openCards: Array[Int], val closeCards: Array[Int], val gameOver: Boolean, val scores: Array[Int], val winnerPosition: Option[Int])


class Game(_owner: Player, multiPlayer: Boolean) extends Actor with OnlineUsers with Lobby with DeckShuffler with Invitations {

  val id = _owner.id

  private var _key = new GameKey(id, _owner.points)

  private var _info = new GameInfo(_key, _owner.identity)

  private val players: Array[Player] = Array(_owner, null)

  private val scores = Array(0, 0)

  private var roundActive = false

  private var participantReady = false

  private val random: Random = new Random

  private var settings: GameSettings = null

  private var currentTurn: Int = -1

  private var deck: IndexedSeq[GameCard] = null

  private var previousCard: GameCard = null

  private var game: Play = FirstPlay

  private var full = false

  resetGame(_owner.points)

  if (multiPlayer)
    addWaiting(this)

  start

  def act {
    loop {
      react {
        case LeftBy(user, bySystem) =>
          _leftBy(user, bySystem)
        case JoinedBy(user) =>
          _joinedBy(user)
        case SetGameSettings(user, settings) =>
          _setGameSettings(user, settings)
        case SetParticipantReady(participant: Player) =>
          _setReady(participant)
        case StartRound(cards) =>
          _startRound(cards)
        case StartBy(user) =>
          _startedBy(user)
        case PlayBy(user, index) =>
          _playedBy(user, index)
        case Invite(invitee) =>
          _invite(invitee)
        case GetInvitedBy(inviter) =>
          _getInvitedBy(inviter)
        case Chat(player: User, message: String) =>
          _chat(player, message)
        case msg: Any =>
          Game.logger info "[invalid-game-msg] Game Id: " + id + " , Message: " + msg
      }
    }
  }

  def resetGame(points: Int) {
    roundActive = false
    scores(Game.OwnerPosition) = 0
    scores(Game.ParticipantPosition) = 0
    participantReady = false
    deck = null
    currentTurn = random.nextInt(Game.NumberOfPlayers)
    previousCard = null
    game = FirstPlay
    _key = new GameKey(id, points)
    _info = new GameInfo(_key, players(Game.OwnerPosition).identity)
    full = players(Game.ParticipantPosition) != null
  }

  def key = _key

  def info = _info

  private def owner = players(Game.OwnerPosition)

  private def ownerInfo = new PlayerInfo(owner.identity, Game.OwnerPosition, scores(Game.OwnerPosition))

  private def participantInfo = new PlayerInfo(players(Game.ParticipantPosition).identity, Game.ParticipantPosition, scores(Game.ParticipantPosition))

  private def isOwner(user: Player) = players(Game.OwnerPosition).id == user.id

  private def opponentOf(user: Player) = players((players.indexOf(user) + 1) % Game.NumberOfPlayers)


  def removeParticipantAndReset {
    players(Game.ParticipantPosition) = null
    resetGame(players(Game.OwnerPosition).points)
  }

  def leftBy(user: User, bySystem: Boolean) = this ! LeftBy(user, bySystem)

  private def _leftBy(leavingUser: User, bySystem: Boolean) {
    if (full) {
      val opponent = opponentOf(leavingUser)

      leavingUser.leaveGame

      if (roundActive) {
        val winnerPoints: Int = deck.size / 8

        opponent.oneMoreWin
        opponent.addSessionPoints(winnerPoints)

        if (!bySystem) {
          leavingUser.oneMoreLeave
          leavingUser.addSessionPoints(-winnerPoints)
        }
      }

      if (isOwner(leavingUser)) {
        // End game.
        opponent.leaveGame
        if (multiPlayer)
          removePlaying(id)
      }
      else {
        // Make owner wait another another leavingUser.
        removeParticipantAndReset
        moveToWaiting(id)
        opponent.setStatus(Waiting)
        full = false
      }

      opponent sendMessage new LeaveGameNotification(bySystem)
    }
    else {
      // Owner left the game while waiting an opponent.
      removeWaiting(id)
      leavingUser.leaveGame
    }
  }

  def joinedBy(user: Player) = this ! JoinedBy(user)

  private def _joinedBy(joiningUser: Player) {
    if (!full) {
      moveToPlaying(id)
      joiningUser.join(this)
      players(Game.ParticipantPosition) = joiningUser
      players(Game.OwnerPosition).setStatus(Playing)
      full = true

      removeInvitation(owner, joiningUser)

      owner sendMessage new JoinGameNotification(participantInfo)
      joiningUser sendMessage new JoinGameResponse(ownerInfo, participantInfo, true, Success.ordinal)
    } else {
      joiningUser sendMessage new JoinGameResponse(null, null, false, GameFull.ordinal)
    }
  }

  def setGameSettings(user: User, settings: GameSettings) = this ! SetGameSettings(user, settings)

  private def _setGameSettings(user: User, newSettings: GameSettings) {
    if (isOwner(user)) {
      if (!roundActive) {
        settings = newSettings
        val notification = new SetGameSettingsNotification(settings)
        players.foreach(_ sendMessage notification)
      } else {
        Game.logger warn "[set-game-settings-request-while-round-active] User Id: " + user.id
      }
    } else {
      Game.logger warn "[set-game-settings-request-from-participant] User Id: " + user.id
    }
  }

  def setReady(participant: Player) {
    this ! SetParticipantReady(participant)
  }

  private def _setReady(participant: Player) {
    if (!isOwner(participant)) {
      if (!participantReady) {
        participantReady = true
        owner sendMessage new OpponentReadyNotification
      } else {
        Game.logger warn "[participant-sent-second-ready-req] User Id: " + participant.id
      }
    } else {
      Game.logger warn "[game-owner-sent-ready-req] User Id: " + participant.id
    }
  }

  def startedBy(user: User) = this ! StartBy(user)

  private def _startedBy(user: User) {
    if (isStartableBy(user)) {
      pickCards(this, settings.deckSize)
    }
  }

  private def isStartableBy(user: User) = isOwner(user) && participantReady && !roundActive && settings != null

  def startRound(cards: Array[Card]) = this ! StartRound(cards)

  private def _startRound(cards: Array[Card]) {
    var roundCards = ArrayBuffer[Card]()
    roundCards ++= cards
    roundCards ++= cards

    roundCards = shuffle(shuffle(shuffle(shuffle(shuffle(roundCards)))))

    deck = roundCards.zipWithIndex.map(each => new GameCard(each._1, each._2))

    participantReady = false
    roundActive = true
    currentTurn = Game.OwnerPosition

    val notification = new StartGameNotification(new GameRound(currentTurn, deck.map(_.name)))

    players.foreach(_ sendMessage notification)
  }

  private def shuffle(cards: ArrayBuffer[Card]) = scala.util.Random.shuffle(cards)

  def playedBy(user: Player, index: Int) = this ! PlayBy(user, index)

  private def _playedBy(user: Player, index: Int) {

    if (players.indexOf(user) == currentTurn) {
      val playedCard = deck(index)

      if (playedCard.playable) {
        val playResult = game.play(currentTurn, previousCard, playedCard)

        val playingTurn = currentTurn
        game = playResult.next
        currentTurn = playResult.nextTurn
        previousCard = playResult.previousCard

        if (playResult.matched) {
          scores(playingTurn) += 1
        }

        val _scores = this.scores.clone

        val winnerPosition = if (scores(playingTurn) == settings.deckSize.cardsToWin)
          Some(playingTurn)
        else if (scores.sum == deck.size / 2)
          Some(Game.DrawPosition)
        else
          None

        winnerPosition match {
          case Some(winnerPosition) if winnerPosition != Game.DrawPosition =>
            val winner = players(winnerPosition)
            val pointsToAdd: Int = deck.size / 4
            val newPoints = winner.points + pointsToAdd
            winner addSessionPoints pointsToAdd
            winner.oneMoreWin
            opponentOf(winner).oneMoreLose
            resetGame(newPoints)
          case Some(winnerPosition) =>
            resetGame(players(Game.OwnerPosition).points)
          case None => // do nothing...
        }

        players.foreach(_ sendMessage new GamePlayNotification(new NexTurn(currentTurn, playingTurn, playResult.matched, playResult.openCards, playResult.closeCards, winnerPosition.isDefined, _scores, winnerPosition)))
      }
    }
  }

  def invite(invited: User) = this ! Invite(invited)

  private def _invite(invited: User) {
    if (!full) {
      invited getInvitedBy owner
    } else {
      owner sendMessage new InviteUserResponse(false, InviterGameFull.ordinal)
    }
  }

  def getInvitedBy(inviter: Player) = this ! GetInvitedBy(inviter)

  private def _getInvitedBy(inviter: Player) {
    if (!full) {
      _joinedBy(inviter)
    } else {
      inviter sendMessage new InviteUserResponse(false, InvitedUserInGame.ordinal)
    }
  }

  def chat(player: User, message: String) = this ! Chat(player, message)

  private def _chat(player: User, message: String) {
    val opponent = opponentOf(player)
    if (opponent != null) {
      opponent sendMessage new GameChatNotification(message)
    }
  }

  override def toString = "Game{" + _key.toString + "}"

  override def exceptionHandler = {
    case e: Exception =>
      Game.logger.error("[exception-in-game] GameId: " + id, e)
  }
}
