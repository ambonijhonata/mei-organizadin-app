package com.tcc.androidnative.core.ui.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun FeedbackMessageCard(
    message: TransientMessage,
    modifier: Modifier = Modifier
) {
    FeedbackMessageCard(
        text = resolveMessageText(message),
        tone = message.tone,
        modifier = modifier
    )
}

@Composable
fun FeedbackMessageCard(
    text: String,
    tone: MessageTone,
    modifier: Modifier = Modifier
) {
    val palette = tone.palette()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = palette.background,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = palette.icon,
                contentDescription = null,
                tint = palette.iconTint,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun resolveMessageText(message: TransientMessage): String {
    if (message.textResId != null) {
        return if (message.textArgs.isEmpty()) {
            stringResource(message.textResId)
        } else {
            stringResource(message.textResId, *message.textArgs.toTypedArray())
        }
    }
    return message.text.orEmpty()
}

private data class TonePalette(
    val background: Color,
    val border: Color,
    val text: Color,
    val iconTint: Color,
    val icon: ImageVector
)

private fun MessageTone.palette(): TonePalette {
    return when (this) {
        MessageTone.SUCCESS -> TonePalette(
            background = Color(0xFFF0FDF4),
            border = Color(0xFFBBF7D0),
            text = Color(0xFF166534),
            iconTint = Color(0xFF16A34A),
            icon = Icons.Outlined.CheckCircle
        )
        MessageTone.WARNING -> TonePalette(
            background = Color(0xFFFFFBEB),
            border = Color(0xFFFDE68A),
            text = Color(0xFF92400E),
            iconTint = Color(0xFFD97706),
            icon = Icons.Outlined.WarningAmber
        )
        MessageTone.ERROR -> TonePalette(
            background = Color(0xFFFEF2F2),
            border = Color(0xFFFECACA),
            text = Color(0xFF991B1B),
            iconTint = Color(0xFFDC2626),
            icon = Icons.Outlined.ErrorOutline
        )
        MessageTone.INFO -> TonePalette(
            background = Color(0xFFEFF6FF),
            border = Color(0xFFBFDBFE),
            text = Color(0xFF1E40AF),
            iconTint = Color(0xFF2563EB),
            icon = Icons.Outlined.Info
        )
    }
}
