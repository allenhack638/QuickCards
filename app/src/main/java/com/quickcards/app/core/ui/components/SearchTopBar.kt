package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.theme.qcElevation1

/** The pill search bar shared by Home and the autofill card list. */
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var value by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val searchContentDescription = stringResource(R.string.cards_search_placeholder)
    val colors = QcTheme.colors

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        if (value.text != query) {
            value = TextFieldValue(query, TextRange(query.length))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcDimens.AppBar)
            .padding(horizontal = QcDimens.Gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = QcDimens.SearchBox)
                .qcElevation1(colors, QcShapes.full)
                .background(colors.surface, QcShapes.full),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(QcDimens.SearchBox)) {
                Icon(
                    imageVector = QcIcons.back,
                    contentDescription = stringResource(R.string.cards_search_close),
                    tint = colors.ink
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cards_search_placeholder),
                        style = QcText.F3,
                        color = colors.inkPh,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = {
                        AppLockController.onUserInteraction()
                        value = it
                        onQueryChange(it.text)
                    },
                    singleLine = true,
                    // Searches can hold card digits. A password-class input is the only IME hint that turns
                    // off suggestions and learning on every keyboard; the text itself stays visible.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    textStyle = QcText.F3.copy(color = colors.ink),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics {
                            contentDescription = searchContentDescription
                        }
                )
            }
            if (value.text.isNotEmpty()) {
                IconButton(onClick = {
                    value = TextFieldValue("")
                    onQueryChange("")
                }, modifier = Modifier.size(QcDimens.SearchBox)) {
                    Icon(
                        imageVector = QcIcons.close,
                        contentDescription = stringResource(R.string.cards_search_clear),
                        tint = colors.ink2
                    )
                }
            }
        }
    }
}
