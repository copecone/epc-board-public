package io.github.copecone.epcboard.data.event.room

import io.github.copecone.epcboard.data.RoomPlayer
import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class PlayerAccuracyChange(val player: RoomPlayer, val hitMargins: Array<Int>, val xAcc: Double): BoardEvent {
    override val op = 5
}