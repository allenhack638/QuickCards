package com.quickcards.app.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class QcSheetProbeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun scrimClosesAPlainSheet() {
        compose.setContent {
            var open by remember { mutableStateOf(true) }
            if (open) {
                QcModalBottomSheet(onDismissRequest = { open = false }) { Text("Probe body") }
            }
        }
        compose.onNodeWithText("Probe body").assertExists()
        compose.onNodeWithContentDescription("Close sheet").performTouchInput { click(androidx.compose.ui.geometry.Offset(centerX, height * 0.3f)) }
        compose.waitForIdle()
        compose.onNodeWithText("Probe body").assertDoesNotExist()
    }

    @Test
    fun scrimClosesATallScrollingSheet() {
        compose.setContent {
            var open by remember { mutableStateOf(true) }
            if (open) {
                QcModalBottomSheet(onDismissRequest = { open = false }) {
                    androidx.compose.foundation.layout.Column(
                        androidx.compose.ui.Modifier
                            .heightIn(max = 560.dp)
                    ) {
                        Text("Tall title")
                        androidx.compose.foundation.layout.Column(
                            androidx.compose.ui.Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        ) { repeat(40) { Text("Row $it") } }
                    }
                }
            }
        }
        val top = compose.onNodeWithText("Tall title").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithContentDescription("Close sheet")
            .performTouchInput { click(androidx.compose.ui.geometry.Offset(centerX, top / 2f)) }
        compose.waitForIdle()
        compose.onNodeWithText("Tall title").assertDoesNotExist()
    }
}
