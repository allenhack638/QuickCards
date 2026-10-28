package com.quickcards.app.feature.cardform

import org.junit.Assert.assertEquals
import org.junit.Test

class CardNameOnCardTest {

    @Test
    fun aHolderRenameResyncsAnAutoSyncedName() {
        assertEquals(
            "JOHN",
            resolveNameOnCard(
                baseNameOnCard = "JON",
                baseOwnerName = "JON",
                currentHolderName = "John"
            )
        )
    }

    @Test
    fun anImportedCustomNameSurvivesANoteEdit() {
        assertEquals(
            "JONATHAN Q PUBLIC",
            resolveNameOnCard(
                baseNameOnCard = "JONATHAN Q PUBLIC",
                baseOwnerName = "JON",
                currentHolderName = "John"
            )
        )
    }

    @Test
    fun aBlankNameUsesTheCurrentHolder() {
        assertEquals(
            "JOHN",
            resolveNameOnCard(
                baseNameOnCard = " ",
                baseOwnerName = "JON",
                currentHolderName = "John"
            )
        )
    }
}
