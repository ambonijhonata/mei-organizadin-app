package com.tcc.androidnative.feature.reports.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage

@Composable
fun ReportMessageCard(
    message: TransientMessage,
    modifier: Modifier = Modifier
) {
    val palette = when (message.tone) {
        MessageTone.SUCCESS -> TonePalette(
            background = Color(0xFFECFDF3),
            border = Color(0xFF86EFAC),
            text = Color(0xFF166534)
        )
        MessageTone.WARNING -> TonePalette(
            background = Color(0xFFFFF7ED),
            border = Color(0xFFFCD34D),
            text = Color(0xFF92400E)
        )
        MessageTone.ERROR -> TonePalette(
            background = Color(0xFFFEF2F2),
            border = Color(0xFFFCA5A5),
            text = Color(0xFFB91C1C)
        )
        MessageTone.INFO -> TonePalette(
            background = Color(0xFFEFF6FF),
            border = Color(0xFFBFDBFE),
            text = Color(0xFF1D4ED8)
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = palette.background,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Text(
            text = message.text,
            color = palette.text,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun ReportInfoBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
        Text(
            text = text,
            color = Color(0xFF1D4ED8),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun ReportCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shadowElevation = 2.dp
    ) {
        Column(content = content)
    }
}

private data class TonePalette(
    val background: Color,
    val border: Color,
    val text: Color
)
