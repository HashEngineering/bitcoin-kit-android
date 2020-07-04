package io.horizontalsystems.indexchainkit

import io.horizontalsystems.bitcoincore.storage.BlockHeader

class IndexBlockHeader(
        version: Int,
        previousBlockHeaderHash: ByteArray,
        merkleRoot: ByteArray,
        timestamp: Long,
        bits: Long,
        nonce: Long,
        val signature: ByteArray?,
        hash: ByteArray) : BlockHeader(version, previousBlockHeaderHash, merkleRoot, timestamp, bits, nonce, hash)



