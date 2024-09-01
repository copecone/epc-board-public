package io.github.copecone.epcboard.data.event

import io.github.copecone.epcboard.data.event.client.Handshake
import io.github.copecone.epcboard.data.event.client.NewLink
import io.github.copecone.epcboard.data.event.room.LevelChange
import io.github.copecone.epcboard.data.event.room.PlayerAccuracyChange
import io.github.copecone.epcboard.data.event.room.PlayerJoin
import io.github.copecone.epcboard.data.event.room.PlayerLeave
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

interface BoardEvent {
    companion object {
        @Suppress("UNCHECKED_CAST")
        operator fun get(op: Int): KSerializer<BoardEvent> = when (op) {
            0 -> Handshake.serializer()
            1 -> NewLink.serializer()
            2 -> LevelChange.serializer()
            3 -> PlayerJoin.serializer()
            4 -> PlayerLeave.serializer()
            5 -> PlayerAccuracyChange.serializer()
            else -> throw SerializationException("Unknown event op: $op")
        } as KSerializer<BoardEvent>
    }

    val op: Int
}