package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType

/** The pill search bar shared by Home and the autofill card list. */
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val searchContentDescription = stringResource(R.string.cards_search_placeholder)

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
            .height(QcDimens.AppBar)
            .padding(horizontal = QcDimens.Gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.SearchBox)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    CircleShape
                )
                .border(
                    width = if (focused) 2.dp else 0.dp,
                    color = if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
                .padding(end = QcDimens.Half),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.back,
                    contentDescription = stringResource(R.string.cards_search_close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cards_search_placeholder_tags),
                        style = QcType.BodyLarge.copy(fontWeight = FontWeight(400)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    // Searches can hold card digits; keep them out of the keyboard's suggestions.
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    textStyle = QcType.BodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight(400)
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics {
                            contentDescription = searchContentDescription
                        }
                )
            }
            if (value.text.isNotEmpty()) {
                IconButton(onClick = {
                    value = TextFieldValue("")
                    onQueryChange("")
                }, modifier = Modifier.size(QcDimens.Touch)) {
                    Icon(
                        imageVector = QcIcons.close,
                        contentDescription = stringResource(R.string.cards_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
