package io.github.copecone.epcboard.data

import kotlinx.serialization.Serializable

@Serializable
data class RawRoomData(
    val level: LevelInfo,
    val id: ULong,
    val name: String,
    val isPlaying: Boolean,
    val isGameStarted: Boolean,
    val shouldShowLevel: Boolean,
    val players: ArrayList<RoomPlayer>
)