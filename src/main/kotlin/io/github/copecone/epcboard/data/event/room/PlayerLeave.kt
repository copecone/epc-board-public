package io.github.copecone.epcboard.data.event.room

import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class PlayerLeave(val playerID: ULong): BoardEvent {
    override val op = 4
}