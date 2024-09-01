package io.github.copecone.epcboard.data

import kotlinx.serialization.Serializable

@Serializable
data class RoomPlayer(
    val id: ULong,
    val userId: String,
    val username: String,
    val avatar: String,
    val state: PlayerState
)

@Serializable
data class PlayerState(
    val isReady: Boolean,
    val progress: Double,
    val accuracy: Double,
    val hitMarginsCount: Array<Int>,
    val xAcc: Double,
    val levelReady: Boolean,
    val isPurePerfect: Boolean,
    val isSpectator: Boolean,
    val isFinished: Boolean,
    val downloadProgress: Double,
    val playState: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerState

        if (isReady != other.isReady) return false
        if (progress != other.progress) return false
        if (accuracy != other.accuracy) return false
        if (!hitMarginsCount.contentEquals(other.hitMarginsCount)) return false
        if (xAcc != other.xAcc) return false
        if (levelReady != other.levelReady) return false
        if (isPurePerfect != other.isPurePerfect) return false
        if (isSpectator != other.isSpectator) return false
        if (isFinished != other.isFinished) return false
        if (downloadProgress != other.downloadProgress) return false
        if (playState != other.playState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isReady.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + accuracy.hashCode()
        result = 31 * result + hitMarginsCount.contentHashCode()
        result = 31 * result + xAcc.hashCode()
        result = 31 * result + levelReady.hashCode()
        result = 31 * result + isPurePerfect.hashCode()
        result = 31 * result + isSpectator.hashCode()
        result = 31 * result + isFinished.hashCode()
        result = 31 * result + downloadProgress.hashCode()
        result = 31 * result + playState
        return result
    }
}