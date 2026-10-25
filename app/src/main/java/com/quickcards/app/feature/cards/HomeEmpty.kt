package com.quickcards.app.feature.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors

@Composable
internal fun NoResult(
    modifier: Modifier,
    searchOn: Boolean,
    query: String
) {
    val hasQuery = searchOn && query.isNotBlank()
    CardsMessage(
        modifier = modifier,
        title = if (hasQuery) {
            stringResource(R.string.cards_no_result_query_title, query)
        } else {
            stringResource(R.string.cards_no_result_filter_title)
        },
        body = stringResource(
            if (hasQuery) {
                R.string.cards_no_result_query_body
            } else {
                R.string.cards_no_result_filter_body
            }
        )
    )
}

/**
 * A vault with cards but nothing to show because everything is archived or
 * hidden-expired. Same centred frame as [NoResult], with the one action that
 * brings the cards back.
 */
@Composable
internal fun EmptyNote(
    modifier: Modifier,
    note: CardsEmptyNote,
    onShowExpired: () -> Unit,
    onOpenArchived: () -> Unit
) {
    when (note) {
        CardsEmptyNote.HIDDEN_EXPIRED -> CardsMessage(
            modifier = modifier,
            title = stringResource(R.string.cards_empty_hidden_expired_title),
            body = stringResource(R.string.cards_empty_hidden_expired_body),
            actionLabel = stringResource(R.string.cards_empty_hidden_expired_action),
            onAction = onShowExpired
        )
        CardsEmptyNote.ONLY_ARCHIVED -> CardsMessage(
            modifier = modifier,
            title = stringResource(R.string.cards_empty_archived_title),
            body = stringResource(R.string.cards_empty_archived_body),
            actionLabel = stringResource(R.string.cards_empty_archived_action),
            onAction = onOpenArchived
        )
        CardsEmptyNote.NONE -> NoResult(
            modifier = modifier,
            searchOn = false,
            query = ""
        )
    }
}

@Composable
private fun CardsMessage(
    modifier: Modifier,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(horizontal = QcDimens.Xl, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = QcType.Title,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(QcDimens.Half))
        Text(
            text = body,
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(QcDimens.Base))
            TextButton(
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = actionLabel,
                    style = QcType.Body.copy(fontWeight = FontWeight(600))
                )
            }
        }
    }
}

@Composable
internal fun EmptyState(
    modifier: Modifier,
    bottomInset: Dp,
    onAdd: () -> Unit,
    onImport: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxHeight < 500.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(
                    start = 40.dp,
                    end = 40.dp,
                    top = if (compact) QcDimens.Base else QcDimens.Gutter,
                    bottom = (if (compact) QcDimens.PassGap else QcDimens.Section) + bottomInset
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            EmptyIllustration(
                modifier = Modifier.size(
                    width = if (compact) 120.dp else 168.dp,
                    height = if (compact) 86.dp else 120.dp
                )
            )
            Spacer(modifier = Modifier.height(if (compact) 10.dp else 28.dp))
            Text(
                text = stringResource(R.string.cards_empty_title),
                style = QcType.Headline,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(if (compact) QcDimens.Half else QcDimens.Base))
            Text(
                text = stringResource(R.string.cards_empty_body),
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 210.dp)
            )
            Spacer(modifier = Modifier.height(if (compact) QcDimens.Gutter else 28.dp))
            Column(
                modifier = Modifier.width(200.dp),
                verticalArrangement = Arrangement.spacedBy(QcDimens.PassGap)
            ) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = QcDimens.Section),
                    colors = qcPrimaryButtonColors()
                ) {
                    Icon(
                        imageVector = QcIcons.add,
                        contentDescription = null,
                        modifier = Modifier.size(QcDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(QcDimens.Base))
                    Text(
                        text = stringResource(R.string.cards_add),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = QcDimens.Section),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = QcIcons.download,
                        contentDescription = null,
                        modifier = Modifier.size(QcDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(QcDimens.Base))
                    Text(
                        text = stringResource(R.string.cards_import),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyIllustration(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outline
    val background = MaterialTheme.colorScheme.background
    Canvas(modifier = modifier) {
        val sx = size.width / 168f
        val sy = size.height / 120f
        val stroke = 2.dp.toPx()

        drawRoundRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(30f * sx, 8f * sy),
            size = Size(128f * sx, 80f * sy),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * sx),
            style = Stroke(width = stroke)
        )
        drawRoundRect(
            color = background,
            topLeft = Offset(10f * sx, 30f * sy),
            size = Size(128f * sx, 82f * sy),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * sx)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(10f * sx, 30f * sy),
            size = Size(128f * sx, 82f * sy),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f * sx),
            style = Stroke(width = stroke)
        )
        drawLine(
            color = color,
            start = Offset(10f * sx, 52f * sy),
            end = Offset(138f * sx, 52f * sy),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(24f * sx, 74f * sy),
            end = Offset(124f * sx, 74f * sy),
            strokeWidth = stroke,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(5f * sx, 5f * sx)
            )
        )
        listOf(24f to 52f, 64f to 86f, 98f to 124f).forEach { (start, end) ->
            drawLine(
                color = color,
                start = Offset(start * sx, 92f * sy),
                end = Offset(end * sx, 92f * sy),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
