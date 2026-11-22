package com.quickcards.app.core.ui

import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.ExpiryStatus
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Turns decrypted cards into [HomeCard]s with the names the tiles show; Home and Archived share it.
 * An auto network is detected from the number, a manual one is looked up by id.
 */
class HomeCardMapper(
    networks: List<CardNetwork>,
    holders: List<Holder>,
    banks: List<Bank>,
    types: List<CardType>,
    tags: List<Tag>,
    private val bankFallback: String
) {
    private val domainNetworks = networks.map { it.toDomain() }
    private val networksById = networks.associateBy { it.id }
    private val holdersById = holders.associateBy { it.id }
    private val banksById = banks.associateBy { it.id }
    private val typesById = types.associateBy { it.id }
    private val tagsById = tags.associateBy { it.id }

    fun map(card: VaultCard, today: LocalDate, soonMonths: Int): HomeCard {
        val network = if (card.networkAuto) {
            CardNumberRules.detect(card.number, domainNetworks)
        } else {
            card.networkId?.let { networksById[it]?.toDomain() }
        }
        return HomeCard(
            card = card,
            bankName = card.bankId?.let { banksById[it]?.name } ?: bankFallback,
            holderName = card.holderId?.let { holdersById[it]?.name }.orEmpty(),
            typeName = card.typeId?.let { typesById[it]?.name },
            network = network,
            networkId = network?.id,
            tagNames = card.tagIds.mapNotNull { tagsById[it]?.name },
            expiryState = ExpiryStatus.expiryState(expiry = card.expiry, today = today, soonMonths = soonMonths)
        )
    }
}

/** Today's date now and again just after each midnight, so expiry chips and due strips roll over. */
val todayFlow: Flow<LocalDate> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now.toLocalDate())
        delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
    }
}
