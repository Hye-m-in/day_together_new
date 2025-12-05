package com.example.day_together.ui.message

import androidx.compose.foundation.background
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.ZoneId
import java.time.LocalDate


import android.Manifest
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.R
import com.example.day_together.data.repository.AppRepository
import com.example.day_together.ui.dialogs.InviteMemberDialog
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    // QuestionRepository 삭제
    viewModel: MessageViewModel = viewModel(factory = MessageViewModel.MessageViewModelFactory(
        AppRepository
    ))
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    //chatRoomId가 설정되면 오늘 질문 한 번 불러오기
    LaunchedEffect(uiState.chatRoomId) {
        val roomId = uiState.chatRoomId
        if (roomId != null) {
            viewModel.publishTodayQuestion()
        }
    }



    // 런타임 권한 요청
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) Toast.makeText(context, "이미지 접근 권한 필요", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    // 미디어 선택 런처
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            viewModel.onEvent(MessageEvent.SendImage(uri))
        }
    }

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
            if (uiState.chatRoomId != null) {
                MessageTopBar(
                    chatRoomName = uiState.chatRoomName.toString(),
                    onMoreOptionsClick = { navController.navigate(AppDestinations.CHAT_INFO_ROUTE) },
                    onEditChatRoomName = { newName ->
                        viewModel.onEvent(MessageEvent.EditChatRoomName(newName))
                    }
                )
            }
        },
        containerColor = ScreenBackground
    ) { innerPadding ->
        Column(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.chatRoomId == null) {
                EmptyChatRoomScreen(onInviteClick = { viewModel.onEvent(MessageEvent.ShowInviteDialog) })
            } else {
                ChatScreenContent(
                    messages = uiState.messages,
                    currentUserName = uiState.currentUserName,
                    messageText = uiState.messageText,
                    onTextChange = { text -> viewModel.onEvent(MessageEvent.OnMessageTextChanged(text)) },
                    onSendClick = {
                        val text = viewModel.uiState.value.messageText // 입력창 내용 가져오기
                        viewModel.onEvent(MessageEvent.SendMessage(text)) },
                    onClipClick = { mediaPickerLauncher.launch("image/*") }
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
    onClipClick: () -> Unit,
) {
    if (messages.isEmpty()) {
        // 메세지가 없을 때
        EmptyChatMessagesView(
            modifier = Modifier.weight(1f)
        )

    } else {
        // 메세지가 있을 때
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            itemsIndexed(
                items = messages,
                key = { _, msg -> msg.timestamp.time }
            ) { index, message ->

                val currentDate: LocalDate = remember(message.timestamp) {
                    message.timestamp.toInstant()
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toLocalDate()
                }

                val previousDate: LocalDate? =
                    if (index > 0) {
                        messages[index - 1].timestamp.toInstant()
                            .atZone(ZoneId.of("Asia/Seoul"))
                            .toLocalDate()
                    } else {
                        null
                    }

                if (index == 0 || currentDate != previousDate) {
                    DateHeader(date = message.timestamp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                MessageBubble(
                    message = message,
                    isMine = message.sender == currentUserName
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

    }
    MessageInputArea(
        text = messageText,
        onTextChanged = onTextChange,
        onSendClick = onSendClick,
        onClipClick = onClipClick
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTopBar(
    chatRoomName: String,
    onMoreOptionsClick: () -> Unit,
    onEditChatRoomName: (String) -> Unit // 이름 수정 콜백
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(chatRoomName) }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEditing) {
                    TextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.widthIn(min = 100.dp, max = 200.dp),
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                isEditing = false
                                onEditChatRoomName(editedName)
                            }
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                    )
                } else {
                    Text(
                        text = chatRoomName,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    if (isEditing) {
                        // 편집 모드 종료 → 저장
                        isEditing = false
                        onEditChatRoomName(editedName)
                    } else {
                        // 편집 모드 진입
                        editedName = chatRoomName
                        isEditing = true
                    }
                }) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                        contentDescription = if (isEditing) "이름 저장" else "이름 수정"
                    )
                }
            }
        },
        actions = {

            IconButton(onClick = onMoreOptionsClick) {
                Icon(
                    painterResource(id = R.drawable.ic_more_options),
                    contentDescription = "더보기"
                )
            }
        }
    )
}


@Composable
fun EmptyChatRoomScreen(onInviteClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("참여 중인 채팅방이 없습니다.\n가족을 초대해 새로운 채팅방을 만들어 보세요.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onInviteClick) {
                Text("가족 초대하기")
            }
        }
    }
}

@Composable
fun EmptyChatMessagesView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "아직 대화가 없습니다.\n첫 메세지를 보내보세요!",
                textAlign = TextAlign.Center
            )
        }
    }
}




@Composable
fun MessageBubble(message: ChatMessage, isMine: Boolean) {

    // 전송 시간 포맷팅
    val timeText = remember(message.timestamp) {
        val localDate = message.timestamp
        val formatter = SimpleDateFormat("a h:mm", Locale.KOREA)
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        formatter.format(localDate)
    }

    // 시스템 메시지 여부 판단 변수 추가
    val isSystem = message.sender == "system"
    // 오늘의 질문 채팅화면에서의 이름
    val displayName = when{
        isSystem -> "오늘의 질문"
        else -> message.sender
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (!isMine) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }

        Row(verticalAlignment = Alignment.Bottom) {
            if (!isMine) {

                SenderBubble(
                    message = message,
                    isMine = false,
                    isSystem = isSystem,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))
                MessageTime(timeText)

            } else {
                MessageTime(timeText)
                Spacer(modifier = Modifier.width(6.dp))

                SenderBubble(
                    message = message,
                    isMine = true,
                    isSystem = false,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}


@Composable
private fun SenderBubble(
    message: ChatMessage,
    isMine: Boolean = false,
    isSystem : Boolean,
    modifier: Modifier = Modifier) {

    // 말풍선 색상 / 글자색 분기
    val bubbleColor: Color
    val textColor: Color

    when {
        isSystem -> { // GPT 질문
            bubbleColor = Color.White
            textColor = MaterialTheme.colorScheme.onSurface
        }
        isMine -> {  // 내 메세지
            bubbleColor = MaterialTheme.colorScheme.primary
            textColor = MaterialTheme.colorScheme.onPrimary
        }
        else -> {    // 가족 메세지
            bubbleColor = Color(0x40F2E0B4)
            textColor = MaterialTheme.colorScheme.onSurface
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bubbleColor,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {

            // 이미지 있을 때
            if (!message.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = message.imageUrl,
                    contentDescription = "Image message",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 텍스트 메시지
            if (!message.content.isNullOrEmpty()) {
                Text(
                    text = message.content,
                    color = textColor
                )
            }
        }
    }
}


@Composable
private fun MessageTime(timeText: String) {
    Text(
        text = timeText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}


@Composable
fun DateHeader(date: Date) {
    val dateText = remember(date) {
        val formatter = SimpleDateFormat("yyyy년 MM월 dd일 EEEE", Locale.KOREA)
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        formatter.format(date)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = 0.85f)
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E7EB))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// 입력창 컴포저블
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