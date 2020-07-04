package io.horizontalsystems.indexchainkit.messages

import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.io.BitcoinOutput
import io.horizontalsystems.bitcoincore.models.NetworkAddress
import io.horizontalsystems.bitcoincore.network.messages.IMessage
import io.horizontalsystems.bitcoincore.network.messages.IMessageParser
import io.horizontalsystems.bitcoincore.network.messages.IMessageSerializer
import io.horizontalsystems.bitcoincore.network.messages.VersionMessage
import io.horizontalsystems.bitcoincore.network.messages.VersionMessageParser
import io.horizontalsystems.bitcoincore.network.messages.VersionMessageSerializer
import java.io.ByteArrayInputStream

class IndexChainVersionMessageParser : VersionMessageParser() {

    override fun parseMessage(payload: ByteArray): IMessage {
        BitcoinInput(ByteArrayInputStream(payload)).use { input ->
            val protocolVersion = input.readInt()
            val services = input.readLong()
            val timestamp = input.readLong()
            val recipientAddress = NetworkAddress.parse(input, false)

            val versionMessage = VersionMessage(protocolVersion, services, timestamp, recipientAddress)

            versionMessage.senderAddress = NetworkAddress.parse(input, false)
            versionMessage.nonce = input.readLong()
            versionMessage.subVersion = input.readString()
            versionMessage.lastBlock = input.readInt()
            versionMessage.relay = input.readByte().toInt() != 0

            return versionMessage
        }

    }
}

class IndexChainVersionMessageSerializer : VersionMessageSerializer() {

    override fun serialize(message: IMessage): ByteArray? {
        if (message !is VersionMessage) {
            return null
        }

        val output = BitcoinOutput()
        output.writeInt(message.protocolVersion)
                .writeLong(message.services)
                .writeLong(message.timestamp)
                .write(message.recipientAddress.toByteArray(false))
        output.write(message.senderAddress.toByteArray(false))
                    .writeLong(message.nonce)
                    .writeString(message.subVersion)
                    .writeInt(message.lastBlock)
        output.writeByte(1)


        return output.toByteArray()
    }
}