package com.basrikahveci
package cardgame.messaging.response

import cardgame.domain.game.GameSettings
import cardgame.messaging.{Success, Response}

class SetGameSettingsNotification(val settings: GameSettings, val success: Boolean = true, val reason: Int = Success.ordinal) extends Response
