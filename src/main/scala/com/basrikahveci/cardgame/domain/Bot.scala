package com.basrikahveci
package cardgame.domain

import cardgame.messaging.Response
import cardgame.messaging.response.{GamePlayNotification, StartGameNotification}
import actors.Actor
import collection.mutable.ArrayBuffer
import java.util.concurrent.{TimeUnit, Executors}

case class SendMessage(response: Response)

case object LeaveGameNotification

object Bot {
  val Executor = Executors.newScheduledThreadPool(4)
}

class Bot(_game: Game) extends Player with Actor {

  start

  def act {
    loop {
      react {
        case SendMessage(response) =>
          _sendMessage(response)
        case LeaveGameNotification =>
          _leaveGame
      }
    }
  }

  var closedCards = scala.collection.mutable.ArrayBuffer[Int]()

  def id = -1L

  val identity = new UserIdentity(id, "Bot", Playing.ordinal)

  var game: Option[Game] = Some(_game)

  def points = 0

  def setStatus(status: UserStatus) {}

  def oneMoreWin {}

  def oneMoreLose {}

  def oneMoreLeave {}

  def addSessionPoints(pointsToAdd: Int) {}

  def join(game: Game) {}

  def leaveGame = this ! LeaveGameNotification

  private def _leaveGame = game = None

  def sendMessage(response: Response) = this ! SendMessage(response)

  var rememberedCards = ArrayBuffer[Int]()

  var previousCard: Option[Int] = None

  var cards = ArrayBuffer[String]()

  var numberOfCardsToRemember: Int = 0

  private def _sendMessage(response: Response) {
    Bot.Executor.schedule(new Runnable {
      def run() {
        response match {
          case startGameNotification: StartGameNotification =>
            closedCards.clear()

            startGameNotification.round.cards.zipWithIndex.foreach(eachCard => closedCards += eachCard._2)
            closedCards = scala.util.Random shuffle closedCards

            cards.clear()
            rememberedCards.clear()
            cards ++= startGameNotification.round.cards
            numberOfCardsToRemember = cards.size / 5

            if (startGameNotification.round.currentTurn == 1) {
              play(closedCards(0))
            }

          case gamePlayNotification: GamePlayNotification =>
            if (!gamePlayNotification.nextTurn.gameOver) {
              gamePlayNotification.nextTurn.openCards.foreach {
                openCard =>
                  closedCards -= openCard
                  rememberedCards -= openCard
              }

              gamePlayNotification.nextTurn.closeCards.foreach {
                closeCard =>
                  closedCards += closeCard
                  if (!rememberedCards.contains(closeCard)) {
                    rememberedCards += closeCard
                  }
              }

              if (rememberedCards.size > numberOfCardsToRemember) {
                rememberedCards = rememberedCards.drop(rememberedCards.size - numberOfCardsToRemember)
              }

              if (gamePlayNotification.nextTurn.nextTurnPosition == 1) {
                previousCard match {
                  case Some(prev) =>
                    rememberedCards.find(cards(_) == cards(prev)) match {
                      case Some(matching) =>
                        game.foreach(_.playedBy(Bot.this, matching))
                      case None =>
                        game.foreach(_.playedBy(Bot.this, closedCards(0)))
                    }
                    previousCard = None
                  case None =>
                    val previousMatch =
                      for (prev1 <- rememberedCards; prev2 <- rememberedCards if (prev1 != prev2 && cards(prev1) == cards(prev2)))
                      yield prev1

                    if (!previousMatch.isEmpty) {
                      play(previousMatch.head)
                    } else {
                      play(closedCards(0))
                    }
                }
              }
            } else {
              game.foreach(_.setReady(Bot.this))
            }
          case _ =>
        }
      }
    }, 1, TimeUnit.SECONDS)
  }

  private def play(card: Int) {
    game.foreach(_.playedBy(this, card))
    previousCard = Some(card)
  }

  override def exceptionHandler = {
    case e: Exception =>
      User.logger.error("[exception-in-bot] Game Id: " + (if (game.isDefined) game.get.id else "-"), e)
  }

  override def hashCode() = id.hashCode()

  override def equals(other: Any) = other match {
    case u: Player => id == u.id
    case _ => false
  }
}
