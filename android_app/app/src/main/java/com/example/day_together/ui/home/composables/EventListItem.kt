package com.example.day_together.ui.home.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.day_together.data.model.CalendarEvent
import com.example.day_together.ui.theme.TextPrimary

@Composable
fun EventListItem(
    event: CalendarEvent,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column을 사용해 제목과 작성자 이름을 세로로 배열
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                // event.description -> event.title 로 변경
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 작성자 이름 표시
            if (event.creatorName.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "작성자: ${event.creatorName}",
                    style = MaterialTheme.typography.bodySmall, // 제목보다 작은 글씨
                    color = TextPrimary.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "일정 옵션 더보기",
                    tint = TextPrimary.copy(alpha = 0.7f)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("수정") },
                    onClick = {
                        onEditClick()
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("삭제") },
                    onClick = {
                        onDeleteClick()
                        showMenu = false
                    }
                )
            }
        }
    }
}