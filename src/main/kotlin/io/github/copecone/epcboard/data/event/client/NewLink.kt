package io.github.copecone.epcboard.data.event.client

import io.github.copecone.epcboard.data.RoomInfo
import io.github.copecone.epcboard.data.event.BoardEvent
import kotlinx.serialization.Serializable

@Serializable
class NewLink(val current: RoomInfo): BoardEvent {
    override val op = 1
}