package com.quickcards.app.feature.cardform

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quickcards.app.R
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme

@Composable
fun CardFormScreen(
    navController: NavController,
    onOpenCard: (cardId: String) -> Unit,
    viewModel: CardFormViewModel = viewModel()
) {
    val session by viewModel.session.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val cardId = viewModel.cardId
    var editReady by remember { mutableStateOf(cardId == null) }
    var showDiscard by remember { mutableStateOf(false) }
    val locked = LocalAppLocked.current

    LaunchedEffect(locked) {
        if (locked) showDiscard = false
    }

    fun requestBack() {
        if (session?.isDirty() == true) {
            showDiscard = true
        } else {
            focusManager.clearFocus(force = true)
            navController.popBackStack()
        }
    }

    BackHandler(enabled = true, onBack = ::requestBack)

    LaunchedEffect(cardId) {
        if (cardId != null) {
            if (!UnlockSession.isUnlocked.value) {
                val activity = context as? androidx.fragment.app.FragmentActivity
                val granted = activity?.let {
                    UnlockSession.requireUnlock(it, context.getString(R.string.cards_unlock_edit))
                } ?: false
                if (!granted) {
                    navController.popBackStack()
                    return@LaunchedEffect
                }
            }
            editReady = true
        }
    }

    val mode = session?.form?.mode ?: if (cardId == null) CardFormMode.ADD else CardFormMode.EDIT
    Column(modifier = Modifier.fillMaxSize().background(QcTheme.colors.bg)) {
        FormTopBar(
            title = stringResource(if (mode == CardFormMode.EDIT) R.string.form_title_edit else R.string.form_title_add),
            scrolled = scrollState.value > 0,
            onBack = ::requestBack
        )
        val current = session
        if (editReady && current != null) {
            CardFormContent(
                viewModel = viewModel,
                session = current,
                saving = saving,
                navController = navController,
                scrollState = scrollState,
                onOpenCard = onOpenCard
            )
        } else {
            CardFormSkeleton(mode = mode, modifier = Modifier.weight(1f))
        }
    }

    if (showDiscard) {
        QcDialog(
            onDismissRequest = { showDiscard = false },
            title = stringResource(R.string.form_discard_title),
            actions = {
                QcTextButton(
                    text = stringResource(R.string.form_discard_keep),
                    onClick = { showDiscard = false },
                    tone = QcTextTone.Neutral
                )
                QcTextButton(
                    text = stringResource(R.string.form_discard_confirm),
                    onClick = {
                        showDiscard = false
                        focusManager.clearFocus(force = true)
                        navController.popBackStack()
                    },
                    tone = QcTextTone.Danger
                )
            }
        )
    }
}

/** Nested bar (64): back and the `f4` title; a hairline shows once the form scrolls under it. */
@Composable
private fun FormTopBar(title: String, scrolled: Boolean, onBack: () -> Unit) {
    val colors = QcTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = QcSize.bar)
                .padding(horizontal = QcSpacing.s1),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(QcSize.hit)) {
                Icon(
                    imageVector = QcIcons.back,
                    contentDescription = stringResource(R.string.form_back),
                    tint = colors.ink,
                    modifier = Modifier.size(QcSize.iconLg)
                )
            }
            Text(
                text = title,
                style = QcText.F4,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = QcSpacing.s3)
                    .semantics { heading() }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcSize.hairline)
                .background(if (scrolled) colors.line else colors.bg)
        )
    }
}
