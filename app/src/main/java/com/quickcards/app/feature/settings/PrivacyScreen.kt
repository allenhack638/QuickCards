package com.quickcards.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/**
 * The privacy policy is bundled as a string resource and rendered in the app, so the Play
 * requirement for an in-app policy is met without adding a WebView or the INTERNET permission.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val colors = QcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .navigationBarsPadding()
    ) {
        QcNestedTopBar(
            onBack = onBack,
            backLabel = stringResource(R.string.settings_privacy_back),
            title = stringResource(R.string.settings_privacy_title)
        )
        SelectionContainer {
            Text(
                text = stringResource(R.string.privacy_policy_body),
                style = QcText.F2,
                color = colors.ink,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s4, bottom = QcSpacing.s6)
            )
        }
    }
}
