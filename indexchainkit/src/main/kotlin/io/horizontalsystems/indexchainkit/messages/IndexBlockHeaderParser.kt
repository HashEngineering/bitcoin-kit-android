package io.horizontalsystems.indexchainkit.messages

import io.horizontalsystems.bitcoincore.core.IHasher
import io.horizontalsystems.bitcoincore.io.BitcoinInput
import io.horizontalsystems.bitcoincore.serializers.BlockHeaderParser
import io.horizontalsystems.bitcoincore.storage.BlockHeader
import io.horizontalsystems.indexchainkit.IndexBlockHeader
import io.horizontalsystems.indexchainkit.X16Rv2Hasher

class IndexBlockHeaderParser(hasher: IHasher = X16Rv2Hasher()) : BlockHeaderParser(hasher) {

    override fun parse(input: BitcoinInput): BlockHeader {
        val version = input.readInt()
        val previousBlockHeaderHash = input.readBytes(32)
        val merkleRoot = input.readBytes(32)
        val timestamp = input.readUnsignedInt()
        val bits = input.readUnsignedInt()
        val nonce = input.readUnsignedInt()
        var signature: ByteArray? = null

        if (nonce == 0L) {
            val signatureSize = input.readVarInt()
            signature = input.readBytes(signatureSize.toInt())
        }
        val payload = serialize(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce)

        val hash = hasher.hash(payload)

        return IndexBlockHeader(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce, signature, hash)
    }

}
