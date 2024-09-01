package io.github.copecone.epcboard.data.event.room

import io.github.copecone.epcboard.data.RoomPlayer
import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class PlayerJoin(val player: RoomPlayer) : BoardEvent {
    override val op = 3
}