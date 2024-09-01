package io.github.copecone.epcboard.data.event.client

import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class Handshake: BoardEvent {
    override val op = 0
}