package com.quickcards.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R

/** "Credit · Aarav Mehta": the non-blank [parts] joined by the one meta separator. */
@Composable
fun metaLine(vararg parts: String?): String =
    parts.filterNot { it.isNullOrBlank() }.joinToString(stringResource(R.string.common_meta_separator))
