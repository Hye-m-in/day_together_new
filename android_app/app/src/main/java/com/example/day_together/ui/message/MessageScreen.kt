package com.example.day_together.ui.message

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.day_together.AppDestinations
import com.example.day_together.ui.message.ChatMessage
import com.example.day_together.R
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.ui.dialogs.InviteMemberDialog
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MessageViewModel = viewModel(factory = MessageViewModelFactory(AppRepository))
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showInviteDialog) {
        InviteMemberDialog(
            onDismissRequest = { viewModel.onEvent(MessageEvent.DismissInviteDialog) },
            onInviteClick = { email ->
                if (email.isNotBlank()) {
                    viewModel.onEvent(MessageEvent.InviteMember(email))
                } else {
                    Toast.makeText(context, "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        topBar = {
            MessageTopBar(
                onInviteClick = { viewModel.onEvent(MessageEvent.ShowInviteDialog) },
                onMoreOptionsClick = { navController.navigate(AppDestinations.CHAT_INFO_ROUTE) }
            )
        },
        containerColor = ScreenBackground
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.chatRoomId == null) {
                EmptyChatRoomScreen(onCreateClick = { viewModel.onEvent(MessageEvent.CreateNewChatRoom) })
            } else {
                ChatScreenContent(
                    messages = uiState.messages,
                    currentUserName = uiState.currentUserName,
                    messageText = uiState.messageText,
                    onTextChange = { text -> viewModel.onEvent(MessageEvent.OnMessageTextChanged(text)) },
                    onSendClick = { viewModel.onEvent(MessageEvent.SendMessage) },
                    onInviteClick = { viewModel.onEvent(MessageEvent.ShowInviteDialog) }
                )
            }
        }
    }
}

@Composable
fun ColumnScope.ChatScreenContent(
    messages: List<ChatMessage>,
    currentUserName: String,
    messageText: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    if (messages.isEmpty()) {
        EmptyChatMessagesView(modifier = Modifier.weight(1f), onInviteClick = onInviteClick)
    } else {
        LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
            items(messages, key = { it.timestamp.time }) { message ->
                MessageBubble(message = message, isMine = message.sender == currentUserName)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    MessageInputArea(
        text = messageText,
        onTextChanged = onTextChange,
        onSendClick = onSendClick,
        onClipClick = { /* TODO: Attachment panel event */ }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTopBar(
    onInviteClick: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    TopAppBar(
        title = { Text("가족 채팅방", fontWeight = FontWeight.Bold) },
        actions = {
            IconButton(onClick = onInviteClick) {
                Icon(Icons.Default.Add, contentDescription = "가족 초대")
            }
            IconButton(onClick = onMoreOptionsClick) {
                Icon(painterResource(id = R.drawable.ic_more_options), contentDescription = "더보기")
            }
        }
    )
}

@Composable
fun EmptyChatRoomScreen(onCreateClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("참여 중인 채팅방이 없습니다.\n새로운 채팅방을 만들어 대화를 시작해보세요.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onCreateClick) {
                Text("채팅방 만들기")
            }
        }
    }
}

@Composable
fun EmptyChatMessagesView(modifier: Modifier = Modifier, onInviteClick: () -> Unit) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("아직 대화가 없습니다.\n가족을 초대해 대화를 시작해보세요.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onInviteClick) {
                Text("가족 초대")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMine: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text = message.sender,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onClipClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClipClick) {
                Icon(painterResource(id = R.drawable.ic_clip_attach), contentDescription = "파일 첨부")
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("메시지 입력") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )
            IconButton(onClick = onSendClick, enabled = text.isNotBlank()) {
                Icon(painterResource(id = R.drawable.ic_send_arrow), contentDescription = "전송")
            }
        }
    }
}