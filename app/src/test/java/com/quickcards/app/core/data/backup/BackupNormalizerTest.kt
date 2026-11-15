package com.quickcards.app.core.data.backup

import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupNetwork
import com.quickcards.domain.CardField
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupNormalizerTest {

    private val localNumber = ("88" + "1".repeat(13)).let { it + CardNumberRules.luhnCheckDigit(it) }
    private val localDebit = BackupNetwork(
        id = "local-debit",
        name = "Local Debit",
        code = "LOCAL",
        prefixes = "88",
        lengths = "16",
        cvv = 3,
        cvvOptional = true
    )

    private fun normalized(networks: List<BackupNetwork>, cvv: String = "") =
        BackupNormalizer.normalize(
            BackupContent(networks = networks, cards = listOf(BackupCard(number = localNumber, exp = "12/29", cvv = cvv))),
            DefaultNetworks.all
        ).cards.single().result

    @Test
    fun aCardOnANetworkOnlyInTheBackupFollowsThatNetworksRules() {
        assertTrue(normalized(listOf(localDebit)).needsReview.isEmpty())
    }

    @Test
    fun withoutItsNetworkTheBlankCvvStillNeedsACheck() {
        assertEquals(setOf(CardField.CVV), normalized(emptyList()).needsReview)
    }

    @Test
    fun theBackupCannotLoosenANetworkThePhoneAlreadyHas() {
        val looseVisa = localDebit.copy(id = "x", name = "visa", prefixes = "4", cvvOptional = true)
        val result = BackupNormalizer.normalize(
            BackupContent(networks = listOf(looseVisa), cards = listOf(BackupCard(number = "4111111111111111", exp = "12/29", cvv = ""))),
            DefaultNetworks.all
        ).cards.single().result
        assertEquals(setOf(CardField.CVV), result.needsReview)
    }

    /** R1 #2: a backup-only network sharing Mastercard's 51 prefix; the card is explicitly on it. */
    private val store = localDebit.copy(id = "store", name = "Store", code = "STORE", prefixes = "51")
    private val storeNumber = ("51" + "1".repeat(13)).let { it + CardNumberRules.luhnCheckDigit(it) }

    private fun storeCard(cvv: String, networkAuto: Boolean) = BackupNormalizer.normalize(
        BackupContent(
            networks = listOf(store),
            cards = listOf(
                BackupCard(
                    number = storeNumber,
                    exp = "12/29",
                    cvv = cvv,
                    networkAuto = networkAuto,
                    networkId = if (networkAuto) null else store.id
                )
            )
        ),
        DefaultNetworks.all
    ).cards.single().result

    @Test
    fun aCardExplicitlyOnABackupNetworkIsCheckedAgainstItOnAPrefixTie() {
        assertTrue(storeCard(cvv = "", networkAuto = false).needsReview.isEmpty())
    }

    @Test
    fun anAutoCardOnThePrefixTieStillFollowsDetection() {
        assertEquals(setOf(CardField.CVV), storeCard(cvv = "", networkAuto = true).needsReview)
    }

    @Test
    fun aBadCvvOnTheExplicitNetworkIsStillFlagged() {
        assertEquals(setOf(CardField.CVV), storeCard(cvv = "12", networkAuto = false).needsReview)
    }
}
