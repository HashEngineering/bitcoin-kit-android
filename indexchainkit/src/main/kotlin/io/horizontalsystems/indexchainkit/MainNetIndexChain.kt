package io.horizontalsystems.indexchainkit

import io.horizontalsystems.bitcoincore.network.Network

class MainNetIndexChain : Network() {
    override val protocolVersion: Int = 90039
    override val noBloomVersion: Int = 90013
    override var port: Int = 7082
    override var magic: Long = 0x4df7d3e5
    override var bip32HeaderPub: Int = 0x0488B21E   // The 4 byte header that serializes in base58 to "xpub".
    override var bip32HeaderPriv: Int = 0x0488ADE4  // The 4 byte header that serializes in base58 to "xprv"
    override var addressVersion: Int = 102
    override var addressSegwitHrp: String = "ic"
    override var addressScriptVersion: Int = 0x07
    override var coinType: Int = 475
    override val maxBlockSize = 2_000_000
    override val dustRelayTxFee = 3000

    // https://github.com/bitcoin/bitcoin/blob/c536dfbcb00fb15963bf5d507b7017c241718bf6/src/policy/policy.h#L50
    override val syncableFromApi = true
    override var dnsSeeds = listOf(
            "idxseeder.mineit.io"
    )
}