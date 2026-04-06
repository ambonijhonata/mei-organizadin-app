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
import androidx.compose.ui.unit.dp
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.ui.feedback.TransientMessage

@Composable
fun ReportMessageCard(
    message: TransientMessage,
    modifier: Modifier = Modifier
) {
    FeedbackMessageCard(
        message = message,
        modifier = modifier
    )
}

@Composable
fun ReportInfoBanner(
    text: String,
    modifier: Modifier = Modifier
) {
    FeedbackMessageCard(text = text, tone = MessageTone.INFO, modifier = modifier)
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
