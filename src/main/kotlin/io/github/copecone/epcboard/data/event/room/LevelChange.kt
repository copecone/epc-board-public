package io.github.copecone.epcboard.data.event.room

import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class LevelChange: BoardEvent {
    override val op = 2
}