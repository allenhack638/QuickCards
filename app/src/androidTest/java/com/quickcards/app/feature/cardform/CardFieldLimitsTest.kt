package com.quickcards.app.feature.cardform

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.domain.DefaultNetworks
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth

/** Fields show the filtered value, not the raw input (Amex CVV 4 digits, unknown number 19). */
@RunWith(AndroidJUnit4::class)
class CardFieldLimitsTest {

    @get:Rule
    val compose = createComposeRule()

    private fun newSession() = CardFormSession.forAdd(
        lists = CardFormLists(networks = DefaultNetworks.all),
        options = CardFormOptions(),
        today = YearMonth.of(2026, 10)
    )

    private fun editableText(tag: String): String =
        compose.onNodeWithTag(tag)
            .fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    private fun setFields() {
        var session by mutableStateOf(newSession())
        var number by mutableStateOf(TextFieldValue(""))
        var cvv by mutableStateOf(TextFieldValue(""))

        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                Column {
                    FormTextField(
                        value = number,
                        onValueChange = { input ->
                            session = session.updateNumber(input.text)
                            val next = CardFieldInput.number(
                                input.text,
                                session.lists.networks,
                                typing = true
                            )
                            number = TextFieldValue(next, TextRange(next.length))
                        },
                        label = "Card number",
                        modifier = Modifier.padding(16.dp),
                        fieldTag = "number"
                    )
                    FormTextField(
                        value = cvv,
                        onValueChange = { input ->
                            session = session.updateCvv(input.text)
                            val next = CardFieldInput.cvv(
                                input.text,
                                session.form.network(session.lists.networks)
                            )
                            cvv = TextFieldValue(next, TextRange(next.length))
                        },
                        label = "CVV",
                        modifier = Modifier.padding(16.dp),
                        fieldTag = "cvv"
                    )
                }
            }
        }
    }

    @Test
    fun cvvKeepsOnlyDigitsUpToTheAmexLength() {
        setFields()

        compose.onNodeWithTag("number").performTextInput("371449635398431")
        compose.onNodeWithTag("cvv").performTextInput("ab12xy345678")
        compose.waitForIdle()

        assertEquals("1234", editableText("cvv"))
    }

    @Test
    fun cardNumberStopsAtNineteenDigitsWhenNoNetworkMatches() {
        setFields()

        compose.onNodeWithTag("number")
            .performTextInput("1234567890123456789012345")
        compose.waitForIdle()

        val shown = editableText("number").filter { it in '0'..'9' }
        assertEquals(19, shown.length)
        assertEquals("1234567890123456789", shown)
    }
}
