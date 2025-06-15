package com.example.day_together.ui.home.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.day_together.ui.theme.ButtonActiveBackground
import com.example.day_together.ui.theme.ScreenBackground
import com.example.day_together.ui.theme.TextPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventInputView(
    visible: Boolean,
    targetDate: LocalDate,
    eventDescription: String,
    isEditing: Boolean,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd (E)", Locale.KOREAN)
    val focusManager = LocalFocusManager.current
    val cardBackgroundColor = ScreenBackground


    val headerTitle = if (isEditing) "일정 편집" else "새 일정"

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(animationSpec = tween(durationMillis = 250), initialOffsetY = { it }),
        exit = slideOutVertically(animationSpec = tween(durationMillis = 200), targetOffsetY = { it }),
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, WeeklyCalendarBorderColor.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { focusManager.clearFocus(); onCancel() }) {
                        Text("취소", style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary.copy(alpha = 0.8f)))
                    }
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                    )
                    TextButton(
                        onClick = { if (eventDescription.isNotBlank()) { focusManager.clearFocus(); onSave() } },
                        enabled = eventDescription.isNotBlank()
                    ) {
                        Text(
                            "저장",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = if (eventDescription.isNotBlank()) ButtonActiveBackground else TextPrimary.copy(alpha = 0.4f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }


                OutlinedTextField(
                    value = eventDescription,
                    onValueChange = onDescriptionChange,

                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    label = { Text("제목", style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary.copy(alpha = 0.7f))) },
                    placeholder = { Text("일정 제목을 입력하세요", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary.copy(alpha = 0.5f))) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (eventDescription.isNotBlank()) {
                            focusManager.clearFocus()
                            onSave()
                        } else {
                            focusManager.clearFocus()
                        }
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = TextPrimary,
                        unfocusedIndicatorColor = TextPrimary.copy(alpha = 0.3f),
                        focusedLabelColor = TextPrimary,
                        cursorColor = TextPrimary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary.copy(alpha = 0.8f)
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                )


                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TextPrimary.copy(alpha = 0.05f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "날짜",
                        tint = TextPrimary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = targetDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
