package io.github.copecone.epcboard.data.event.room

import io.github.copecone.epcboard.data.LevelInfo
import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class LevelChange(val level: LevelInfo) : BoardEvent {
    override val op = 2
}