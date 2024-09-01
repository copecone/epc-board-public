package io.github.copecone.epcboard.data.event

import io.github.copecone.epcboard.data.event.client.Handshake
import io.github.copecone.epcboard.data.event.client.NewLink
import io.github.copecone.epcboard.data.event.room.LevelChange
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

interface BoardEvent {
    companion object {
        @Suppress("UNCHECKED_CAST")
        operator fun get(op: Int): KSerializer<BoardEvent> = when (op) {
            0 -> Handshake.serializer()
            1 -> NewLink.serializer()
            2 -> LevelChange.serializer()
            else -> throw SerializationException("Unknown event op: $op")
        } as KSerializer<BoardEvent>
    }

    val op: Int
}