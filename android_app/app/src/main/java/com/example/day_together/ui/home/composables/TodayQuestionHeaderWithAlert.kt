package com.example.day_together.ui.home.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.ui.theme.ErrorRed
import com.example.day_together.ui.theme.TextPrimary
import com.example.day_together.R

@Composable
fun TodayQuestionHeaderWithAlert(isAnsweredByAll: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("오늘의 질문", color = TextPrimary, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, fontSize = 21.sp))
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = if (isAnsweredByAll) R.drawable.ic_happy else R.drawable.ic_sad),
                contentDescription = if (isAnsweredByAll) "답변 완료" else "답변 필요",
                tint = TextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        if (!isAnsweredByAll) {
            Text(
                text = "아직 오늘의 질문에 답변하지 않았어요!",
                color = ErrorRed,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                textAlign = TextAlign.End
            )
        }
    }
}