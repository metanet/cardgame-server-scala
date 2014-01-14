package com.basrikahveci
package cardgame.messaging

import net.liftweb.json.Serialization.{read, write}
import net.liftweb.json.Serialization

import com.weiglewilczek.slf4s.Logger
import request._
import response._
import scala.Some
import net.liftweb.json.ShortTypeHints
import cardgame.domain.game.{LARGE, MEDIUM, SMALL}

object MessageFactory {

  val logger = Logger(classOf[MessageFactory])

  val Instance = new MessageFactory

}

class MessageFactory {

  val messageClasses = List(classOf[SignInRequest], classOf[SignInResponse], classOf[GetGameListRequest], classOf[GameChatRequest], classOf[GameChatNotification], classOf[GetGameListResponse], classOf[UserNotification], classOf[CreateGameRequest], classOf[CreateGameResponse], classOf[JoinGameRequest], classOf[JoinGameResponse], classOf[JoinGameNotification], classOf[LeaveGameNotification], classOf[LeaveGameRequest], classOf[SetGameSettingsRequest], classOf[SetGameSettingsNotification], classOf[OpponentReadyRequest], classOf[OpponentReadyNotification], classOf[StartGameRequest], classOf[StartGameNotification], classOf[GamePlayRequest], classOf[GamePlayNotification], classOf[GetUserProfileRequest], classOf[GetUserProfileResponse], classOf[PlayNowRequest], classOf[InviteUserRequest], classOf[InviteUserNotification], classOf[InviteUserResponse], classOf[AcceptInvitationRequest], classOf[SMALL], classOf[MEDIUM], classOf[LARGE])

  implicit val formats = Serialization.formats(ShortTypeHints(messageClasses))

  def decode(message: String) = {
    var request: Option[Request] = None

    try {
      request = Some(read[Request](message))
    } catch {
      case e: Exception =>
        MessageFactory.logger.error("[message-factory] Decoding failed. Message:" + message, e)
    }

    request
  }

  def encode(response: Response) = write(response)

}
