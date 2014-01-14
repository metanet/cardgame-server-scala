package com.basrikahveci
package cardgame.card

import cardgame.domain.game.DeckSize
import util.Random
import cardgame.domain.Game
import actors.Actor


trait DeckShuffler {

  def pickCards(game: Game, deckSize: DeckSize) = SimpleDeckShuffler.Instance ! PickCards(game, deckSize)

}

object SimpleDeckShuffler {
  val Instance = new SimpleDeckShuffler
}

case class PickCards(game: Game, deckSize: DeckSize)

class SimpleDeckShuffler extends Actor with ImageLoader {

  val cards = imageNames.zipWithIndex.map {
    eachImage => new Card(eachImage._2, eachImage._1)
  }.toArray

  start

  def act() {
    while (true) {
      receive {
        case PickCards(game, deckSize) =>
          _pickCards(game, deckSize)
      }
    }
  }

  private def _pickCards(game: Game, deckSize: DeckSize) = {
    val numberOfCards = deckSize.numberOfCards
    var randomRange = cards.length
    val random = new Random

    for (i <- 0 to numberOfCards) {
      val otherIndex = i + random.nextInt(randomRange)
      randomRange -= 1
      val card = cards(i)
      cards(i) = cards(otherIndex)
      cards(otherIndex) = card
    }

    game.startRound(cards.take(numberOfCards))
  }


}
