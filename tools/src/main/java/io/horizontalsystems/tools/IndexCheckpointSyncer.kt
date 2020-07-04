package io.horizontalsystems.tools

import io.horizontalsystems.bitcoincore.core.IConnectionManager
import io.horizontalsystems.bitcoincore.core.IConnectionManagerListener
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.network.messages.*
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.indexchainkit.messages.IndexChainVersionMessageParser
import io.horizontalsystems.indexchainkit.messages.IndexChainVersionMessageSerializer
import io.horizontalsystems.indexchainkit.messages.IndexHeadersMessageParser
import io.horizontalsystems.indexchainkit.messages.IndexHeadersMessageSerializer
import io.horizontalsystems.indexchainkit.MainNetIndexChain
import io.horizontalsystems.indexchainkit.X16Rv2Hasher
import java.lang.IllegalArgumentException

class IndexCheckpointSyncer(
        network: Network,
        checkpointInterval: Int,
        checkpointsToKeep: Int,
        listener: Listener)
    : CheckpointSyncer(network, checkpointInterval, checkpointsToKeep, listener),
        PeerGroup.Listener, IPeerTaskHandler {

    init {
        val blockHeaderHasher = when (network) {
            is MainNetIndexChain -> X16Rv2Hasher()
            else -> throw IllegalArgumentException("network must be indexchain")
        }

        val networkMessageParser = NetworkMessageParser(network.magic).apply {
            add(IndexChainVersionMessageParser())
            add(VerAckMessageParser())
            add(InvMessageParser())
            add(IndexHeadersMessageParser(blockHeaderHasher))
        }

        val networkMessageSerializer = NetworkMessageSerializer(network.magic).apply {
            add(IndexChainVersionMessageSerializer())
            add(VerAckMessageSerializer())
            add(InvMessageSerializer())
            add(GetHeadersMessageSerializer())
            add(IndexHeadersMessageSerializer())
        }

        val connectionManager = object : IConnectionManager {
            override val listener: IConnectionManagerListener? = null
            override val isConnected = true
        }

        val peerHostManager = PeerAddressManager(network)
        peerGroup = PeerGroup(peerHostManager, network, peerManager, peerSize, networkMessageParser, networkMessageSerializer, connectionManager, 0).also {
            peerHostManager.listener = it
        }

        peerGroup.addPeerGroupListener(this)
        peerGroup.peerTaskHandler = this
    }
}
