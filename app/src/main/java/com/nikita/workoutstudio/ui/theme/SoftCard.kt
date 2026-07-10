package com.nikita.workoutstudio.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A soft, frosted-looking surface: subtle vertical gradient + hairline border on
 * rounded corners. Gives the minimalist "blurred glass" feel without runtime blur.
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val gradient = Brush.verticalGradient(
        listOf(
            scheme.surfaceVariant.copy(alpha = 0.95f),
            scheme.surface.copy(alpha = 0.95f)
        )
    )
    // clip() first, then the caller's modifier: a combinedClickable passed in
    // via `modifier` renders its ripple *after* the clip, so the touch highlight
    // is bounded by the rounded corners instead of leaking out as a rectangle.
    Box(
        modifier = Modifier
            .clip(shape)
            .then(modifier)
            .background(gradient)
            .border(1.dp, scheme.outline.copy(alpha = 0.6f), shape)
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
