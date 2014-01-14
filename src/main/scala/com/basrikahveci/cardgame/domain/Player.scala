package com.basrikahveci
package cardgame.domain

import cardgame.messaging.Response

trait Player {

  def id: Long

  def identity: UserIdentity

  def points: Int

  def setStatus(status: UserStatus): Unit

  def oneMoreWin: Unit

  def oneMoreLose: Unit

  def oneMoreLeave: Unit

  def addSessionPoints(pointsToAdd: Int): Unit

  def join(game: Game): Unit

  def leaveGame: Unit

  def sendMessage(response: Response)
}
