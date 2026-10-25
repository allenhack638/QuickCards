package com.quickcards.app.feature.cardform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quickcards.app.R
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType

@Composable
fun CardFormScreen(
    navController: NavController,
    viewModel: CardFormViewModel = viewModel()
) {
    val session by viewModel.session.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val cardId = viewModel.cardId
    var editReady by remember { mutableStateOf(cardId == null) }
    var showDiscard by remember { mutableStateOf(false) }
    val locked = LocalAppLocked.current

    LaunchedEffect(locked) {
        if (locked) {
            showDiscard = false
        }
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
                    UnlockSession.requireUnlock(
                        it,
                        context.getString(R.string.form_unlock_edit_reason)
                    )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FormTopBar(
            title = stringResource(
                if (mode == CardFormMode.EDIT) R.string.form_title_edit else R.string.form_title_add
            ),
            onBack = ::requestBack
        )
        if (editReady && session != null) {
            CardFormContent(
                viewModel = viewModel,
                session = session!!,
                saving = saving,
                navController = navController
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }

    if (showDiscard) {
        DiscardDialog(
            onKeepEditing = { showDiscard = false },
            onDiscard = {
                showDiscard = false
                focusManager.clearFocus(force = true)
                navController.popBackStack()
            }
        )
    }
}

@Composable
private fun DiscardDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    QcDialog(onDismissRequest = onKeepEditing) {
        Text(
            text = stringResource(R.string.form_discard_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))
        Text(
            text = stringResource(R.string.form_discard_body),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogActionGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onKeepEditing,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.form_discard_keep))
            }
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.form_discard_confirm))
            }
        }
    }
}

@Composable
private fun FormTopBar(title: String, onBack: () -> Unit) {
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
                contentDescription = stringResource(R.string.form_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(QcDimens.Half))
        Text(
            text = title,
            style = QcType.NestedTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
