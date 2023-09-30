package io.github.lee0701.mboard.preset.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HexIntSerializer: KSerializer<Int> {
    private const val prefix = "0x"
    override val descriptor = PrimitiveSerialDescriptor("Hexadecimal", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Int) {
        val string = prefix + value.toString(16).padStart(4, '0')
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Int {
        val string = decoder.decodeString()
        return string.replaceFirst(prefix, "").toIntOrNull(16) ?: 0
    }
}