package io.horizontalsystems.indexchainkit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import io.horizontalsystems.bitcoincore.AbstractKit
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.BitcoinCore.SyncMode
import io.horizontalsystems.bitcoincore.BitcoinCoreBuilder
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorChain
import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorSet
import io.horizontalsystems.bitcoincore.core.Bip
import io.horizontalsystems.bitcoincore.managers.*
import io.horizontalsystems.bitcoincore.network.Network
import io.horizontalsystems.bitcoincore.storage.CoreDatabase
import io.horizontalsystems.bitcoincore.storage.Storage
import io.horizontalsystems.bitcoincore.utils.Base58AddressConverter
import io.horizontalsystems.bitcoincore.utils.PaymentAddressParser
import io.horizontalsystems.bitcoincore.utils.SegwitAddressConverter
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.indexchainkit.messages.IndexBlockHeaderParser
import io.horizontalsystems.indexchainkit.messages.IndexChainVersionMessageParser
import io.horizontalsystems.indexchainkit.messages.IndexChainVersionMessageSerializer
import io.horizontalsystems.indexchainkit.validators.DarkGravityWaveTestnetValidator
import io.horizontalsystems.indexchainkit.validators.DarkGravityWaveValidator

class IndexChainKit : AbstractKit {
    enum class NetworkType {
        MainNet,
        TestNet
    }

    interface Listener : BitcoinCore.Listener

    override var bitcoinCore: BitcoinCore
    override var network: Network

    var listener: Listener? = null
        set(value) {
            field = value
            bitcoinCore.listener = value
        }

    constructor(
            context: Context,
            words: List<String>,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44
    ) : this(context, Mnemonic().toSeed(words), walletId, networkType, peerSize, syncMode, confirmationsThreshold, bip)

    constructor(
            context: Context,
            seed: ByteArray,
            walletId: String,
            networkType: NetworkType = NetworkType.MainNet,
            peerSize: Int = 10,
            syncMode: SyncMode = SyncMode.Api(),
            confirmationsThreshold: Int = 6,
            bip: Bip = Bip.BIP44
    ) {
        val database = CoreDatabase.getInstance(context, getDatabaseName(networkType, walletId, syncMode, bip))
        val storage = Storage(database, 5000)
        var initialSyncUrl = ""

        network = when (networkType) {
            NetworkType.MainNet -> {
                initialSyncUrl = "https://insight.indexchain.org/insight-api-zcoin"
                MainNetIndexChain()
            }
            NetworkType.TestNet -> {
                initialSyncUrl = ""
                TestNetIndexChain()
            }
        }

        val paymentAddressParser = PaymentAddressParser("indexchain", removeScheme = true)
        val initialSyncApi = InsightApi(initialSyncUrl)

        val blockValidatorSet = BlockValidatorSet()

        val blockValidatorChain = BlockValidatorChain()

        val blockHelper = BlockValidatorHelper(storage)

        if (network is MainNetIndexChain) {
            blockValidatorChain.add(DarkGravityWaveValidator(blockHelper, dgwPastBlocks, targetTimespan, maxTargetBits, 31))
        } else {
            blockValidatorChain.add(DarkGravityWaveTestnetValidator(targetSpacing, targetTimespan, maxTargetBits, 31))
            blockValidatorChain.add(DarkGravityWaveValidator(blockHelper, dgwPastBlocks, targetTimespan, maxTargetBits, 31))
        }

        blockValidatorSet.addBlockValidator(blockValidatorChain)

        val coreBuilder = BitcoinCoreBuilder()

        bitcoinCore = coreBuilder
                .setContext(context)
                .setSeed(seed)
                .setNetwork(network)
                .setBip(bip)
                .setBlockHeaderParser(IndexBlockHeaderParser())
                .setVersionMessageParser(IndexChainVersionMessageParser())
                .setVersionMessageSerializer(IndexChainVersionMessageSerializer())
                .setPaymentAddressParser(paymentAddressParser)
                .setPeerSize(peerSize)
                .setSyncMode(syncMode)
                .setConfirmationThreshold(confirmationsThreshold)
                .setStorage(storage)
                .setInitialSyncApi(initialSyncApi)
                .setBlockValidator(blockValidatorSet)
                .setBlockHeaderHasher(X16Rv2Hasher())
                .build()

        //  extending bitcoinCore

        val bech32AddressConverter = SegwitAddressConverter(network.addressSegwitHrp)
        val base58AddressConverter = Base58AddressConverter(network.addressVersion, network.addressScriptVersion)

        bitcoinCore.prependAddressConverter(bech32AddressConverter)

        when (bip) {
            Bip.BIP44 -> {
                bitcoinCore.addRestoreKeyConverter(Bip44RestoreKeyConverter(base58AddressConverter))
            }
            Bip.BIP49 -> {
                bitcoinCore.addRestoreKeyConverter(Bip49RestoreKeyConverter(base58AddressConverter))
            }
            Bip.BIP84 -> {
                bitcoinCore.addRestoreKeyConverter(Bip84RestoreKeyConverter(bech32AddressConverter))
            }
        }
    }

    companion object {

        const val maxTargetBits: Long = 0x1e0fffff      // Maximum difficulty
        const val targetSpacing = 120                   // 2.0 minutes per block.
        const val targetTimespan: Long = 3600
        const val dgwPastBlocks: Long = 30
        const val heightInterval = targetTimespan / targetSpacing // 2016 blocks

        private fun getDatabaseName(networkType: NetworkType, walletId: String, syncMode: SyncMode, bip: Bip): String = "Litecoin-${networkType.name}-$walletId-${syncMode.javaClass.simpleName}-${bip.name}"

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            for (syncMode in listOf(SyncMode.Api(), SyncMode.Full(), SyncMode.NewWallet())) {
                for (bip in Bip.values())
                    try {
                        SQLiteDatabase.deleteDatabase(context.getDatabasePath(getDatabaseName(networkType, walletId, syncMode, bip)))
                    } catch (ex: Exception) {
                        continue
                    }
            }
        }
    }

}
