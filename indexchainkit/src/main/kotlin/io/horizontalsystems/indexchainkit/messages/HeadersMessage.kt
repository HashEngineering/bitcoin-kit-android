package io.horizontalsystems.indexchainkit.messages

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoincore.extensions.toReversedHex
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.network.messages.HeadersMessage
import io.horizontalsystems.bitcoincore.network.messages.HeadersMessageParser
import io.horizontalsystems.bitcoincore.network.messages.HeadersMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.indexchainkit.IndexBlockHeader
import java.io.ByteArrayInputStream

class IndexHeadersMessage(val indexHeaders: Array<IndexBlockHeader>) : HeadersMessage( indexHeaders.map { it }.toTypedArray()) {
    override fun toString(): String {
        return "IndexHeadersMessage(${indexHeaders.size}:[${indexHeaders.joinToString { it.hash.toReversedHex() }}])"
    }
}

class IndexHeadersMessageParser(private val hasher: IHasher) : HeadersMessageParser(hasher) {

    override fun parseMessage(payload: ByteArray): IMessage {
        return BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val count = input.readVarInt().toInt()

            val headers = Array(count) {
                val version = input.readInt()
                val prevHash = input.readBytes(32)
                val merkleHash = input.readBytes(32)
                val timestamp = input.readUnsignedInt()
                val bits = input.readUnsignedInt()
                val nonce = input.readUnsignedInt()
                val signature = if (nonce == 0L) {
                    val size = input.readVarInt()
                    input.readBytes(size.toInt())
                } else {
                    null
                }
                input.readVarInt() // tx count always zero

                val headerPayload = BitcoinOutput().also {
                    it.writeInt(version)
                    it.write(prevHash)
                    it.write(merkleHash)
                    it.writeUnsignedInt(timestamp)
                    it.writeUnsignedInt(bits)
                    it.writeUnsignedInt(nonce)
                }

                IndexBlockHeader(version, prevHash, merkleHash, timestamp, bits, nonce, signature, hasher.hash(headerPayload.toByteArray()))
            }

            IndexHeadersMessage(headers)
        }
    }
}

class IndexHeadersMessageSerializer : HeadersMessageSerializer() {

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is HeadersMessage) {
            return null
        }

        val output = BitcoinOutput().also {
            it.writeInt(message.headers.size)
        }

        message.headers.forEach { blockHeader ->
            val it = blockHeader as IndexBlockHeader
            output.writeInt(it.version)
            output.write(it.previousBlockHeaderHash)
            output.write(it.merkleRoot)
            output.writeUnsignedInt(it.timestamp)
            output.writeUnsignedInt(it.bits)
            output.writeUnsignedInt(it.nonce)
            if (it.nonce == 0L)
                output.write(it.signature)
        }

        return output.toByteArray()
    }
}
