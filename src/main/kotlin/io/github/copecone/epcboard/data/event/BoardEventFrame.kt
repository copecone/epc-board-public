package io.github.copecone.epcboard.data.event

import io.github.copecone.epcboard.data.event.client.Handshake
import io.github.copecone.epcboard.util.SerializerUtil
import io.github.copecone.epcboard.util.SerializerUtil.makeStructure
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement

@Serializable(with = BoardEventFrame.Serializer::class)
data class BoardEventFrame(val op: Int, val event: BoardEvent?) {
    object Serializer: KSerializer<BoardEventFrame> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("BoardEventFrame") {
            element<Int>("op")
            element<JsonElement>("data", isOptional = true)
        }

        override fun deserialize(decoder: Decoder): BoardEventFrame {
            var op = 0
            var data: BoardEvent? = null
            decoder.makeStructure(descriptor) { index ->
                when (index) {
                    0 -> op = decodeIntElement(descriptor, index)
                    1 -> {
                        val element = decodeSerializableElement(descriptor, index, JsonElement.serializer())
                        data = SerializerUtil.jsonBuild.decodeFromJsonElement(BoardEvent[op], element)
                    }
                }
            }

            when (op) {
                0 -> data = Handshake()
            }

            return BoardEventFrame(op, data)
        }

        override fun serialize(encoder: Encoder, value: BoardEventFrame) {
            encoder.beginStructure(descriptor).run {
                encodeIntElement(descriptor, 0, value.op)
                value.event?.let { encodeSerializableElement(descriptor, 1, BoardEvent[value.op], value.event) }

                endStructure(descriptor)
            }
        }
    }
}