package io.horizontalsystems.indexchainkit

import io.horizontalsystems.bitcoincore.extensions.hexToByteArray
import io.horizontalsystems.bitcoincore.extensions.toHexString
import org.junit.Assert.assertEquals
import org.junit.Test

class X16Rv2HasherTest {

    @Test
    fun x16Rv2Hashing() {

        val inputHex = listOf(
                "020000000000000000000000000000000000000000000000000000000000000000000000229b2aa28de7e6e4bd2aebcebb1fcf5e98fe7aa089cd82ac9aa30be32551f0b69a88835effff001edfbc0000",
                "00000020327bd75bed329452d22030266c5c688c00b9e55a9faacd2c33c2a73a2600000014ee7a4d3782b8e6f22f01b6e9ee9378ed07269b8bdaa23e799b1dfbefa8d79884d6835effff0f1e08440000",
                "000000201b9c8efd9f4e6be488ddd27826aa39fc4a91bc5a39cbf5742902b21f00000000871b63e7d07545d3746f165bd8391b45629e258e678aef481da9ae7f7467b1f5e0e6835effff0f1e00000000"
        )

        val expectedHex = listOf(
                "000000263aa7c2332ccdaa9f5ae5b9008c685c6c263020d2529432ed5bd77b32",
                "00000136a69e3f5b28164e4fe6e67326654ee182175db11d4995fb66d89400de",
                "6772ee84667e5e6737876effd4a49becba76d0e9db599fa9f2ee2bbf9a40f298"
        )

        val hasher = X16Rv2Hasher()

        inputHex.forEachIndexed { index, hexString ->
            val hex = hexString.hexToByteArray()
            val result = hasher.hash(hex).reversedArray().toHexString()

            assertEquals(expectedHex[index], result)
        }

    }

}
