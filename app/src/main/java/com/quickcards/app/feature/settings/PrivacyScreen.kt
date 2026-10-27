package com.quickcards.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType

/**
 * The privacy policy is bundled as a string resource and rendered in the app, so the Play
 * requirement for an in-app policy is met without adding a WebView or the INTERNET permission.
 */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val policy = stringResource(R.string.privacy_policy_body)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.AppBar)
                .padding(start = QcDimens.Half, end = QcDimens.Half),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.back,
                    contentDescription = stringResource(R.string.settings_privacy_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(QcDimens.Half))
            Text(
                text = stringResource(R.string.settings_privacy_title),
                style = QcType.NestedTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SelectionContainer {
            Text(
                text = policy,
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = QcDimens.Gutter,
                        end = QcDimens.Gutter,
                        top = QcDimens.Gutter,
                        bottom = QcDimens.Section
                    )
            )
        }
    }
}
