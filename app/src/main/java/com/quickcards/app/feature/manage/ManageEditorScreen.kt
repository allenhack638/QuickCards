package com.quickcards.app.feature.manage

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.quickcards.app.R
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.entity.EntityEditorScreen
import com.quickcards.domain.EntityKind

/** The Manage editor: an item in use can't be deleted; an unused one goes at once, with Undo. */
@Composable
fun ManageEditorScreen(
    kind: EntityKind,
    editingId: String?,
    viewModel: ManageViewModel,
    onBack: () -> Unit,
    onShowCards: (EntityKind, String) -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val state by viewModel.state.collectAsState()
    val entity = editingId?.let { id -> state.list(kind).firstOrNull { it.id == id } }
    var blocked by remember { mutableStateOf(false) }
    val locked = LocalAppLocked.current
    LaunchedEffect(locked) { if (locked) blocked = false }

    EntityEditorScreen(
        kind = kind,
        editingId = editingId,
        onBack = onBack,
        onDelete = entity?.let { item ->
            {
                if (item.usageCount > 0) {
                    blocked = true
                } else {
                    viewModel.deleteWithUndo(item) { deleted ->
                        snackbar.showAndAwait(
                            message = context.getString(R.string.manage_deleted, deleted.name),
                            actionLabel = context.getString(R.string.manage_action_undo),
                            duration = SnackbarDuration.Long
                        ) == SnackbarResult.ActionPerformed
                    }
                    onBack()
                }
            }
        }
    )

    if (blocked && entity != null) {
        DeleteBlockedDialog(
            kind = kind,
            entity = entity,
            onDismiss = { blocked = false },
            onShowCards = {
                blocked = false
                onShowCards(kind, entity.id)
            }
        )
    }
}
