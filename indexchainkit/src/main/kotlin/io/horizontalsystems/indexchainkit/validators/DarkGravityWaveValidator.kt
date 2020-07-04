package io.horizontalsystems.indexchainkit.validators

import io.horizontalsystems.bitcoincore.blocks.validators.BlockValidatorException
import io.horizontalsystems.bitcoincore.blocks.validators.IBlockChainedValidator
import io.horizontalsystems.bitcoincore.crypto.CompactBits
import io.horizontalsystems.bitcoincore.managers.BlockValidatorHelper
import io.horizontalsystems.bitcoincore.models.Block
import java.math.BigInteger
import kotlin.math.min

class DarkGravityWaveValidator(
        private val blockHelper: BlockValidatorHelper,
        private val heightInterval: Long,
        private val targetTimespan: Long,
        private val maxTargetBits: Long,
        private val powDGWHeight: Int
) : IBlockChainedValidator {

    override fun validate(block: Block, previousBlock: Block) {

        var actualTimeSpan = 0L
        var avgTargets = BigInteger.ZERO
        var prevBlock: Block? = previousBlock
        val isProofOfStake = block.nonce == 0L
        var lastMatchingProof: Block? = null

        var blockCount = 0
        while (blockCount < heightInterval) {
            var currentBlock = checkNotNull(prevBlock) {
                // Dash DGW throws BlockValidatorException.NoPreviousBlock here
                // but for IndexChain, checkpoints are not exhaustive for both
                // types of blocks
                return
            }

            if ((currentBlock.nonce == 0L) != isProofOfStake) {
                prevBlock = blockHelper.getPrevious(currentBlock, 1)
                checkNotNull(prevBlock) {
                    if (currentBlock.height == 0) {
                        if (maxTargetBits != block.bits) {
                            throw BlockValidatorException.NotEqualBits()
                        } else return
                    }
                    // Dash DGW throws BlockValidatorException.NoPreviousBlock here
                    // but for IndexChain, checkpoints are not exhaustive for both
                    // types of blocks
                    return
                }
                continue
            } else if (lastMatchingProof == null) {
                lastMatchingProof = currentBlock
            }

            avgTargets *= BigInteger.valueOf(blockCount.toLong())
            avgTargets += CompactBits.decode(currentBlock.bits)
            avgTargets /= BigInteger.valueOf(blockCount + 1L)

            ++blockCount
            if (blockCount < heightInterval) {
                prevBlock = blockHelper.getPrevious(currentBlock, 1)
            } else {
                actualTimeSpan = previousBlock.timestamp - currentBlock.timestamp
            }
        }

        if (lastMatchingProof != null)
            lastMatchingProof = previousBlock

        actualTimeSpan = lastMatchingProof!!.timestamp - prevBlock!!.timestamp

        var darkTarget = avgTargets

        if (actualTimeSpan < targetTimespan / 3)
            actualTimeSpan = targetTimespan / 3
        if (actualTimeSpan > targetTimespan * 3)
            actualTimeSpan = targetTimespan * 3

        //  Retarget
        darkTarget = darkTarget * BigInteger.valueOf(actualTimeSpan) / BigInteger.valueOf(targetTimespan)

        val compact = min(CompactBits.encode(darkTarget), maxTargetBits)
        if (compact != block.bits) {
            throw BlockValidatorException.NotEqualBits()
        }
    }

    override fun isBlockValidatable(block: Block, previousBlock: Block): Boolean {
        return block.height >= powDGWHeight
    }
}
