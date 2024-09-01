package io.github.copecone.epcboard.data

import kotlinx.serialization.Serializable

@Serializable
data class LevelInfo(
    val artist: String,
    val song: String,
    val hash: String
)