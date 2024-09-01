package io.github.copecone.epcboard.data

import kotlinx.serialization.Serializable

@Serializable
data class BoardData(val boardID: String, val boardName: String, val boardSocket: String)