package com.example.day_together.ui.message


import androidx.navigation.NavHostController
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.time.LocalDate
import androidx.compose.foundation.BorderStroke
import com.example.day_together.R
import com.example.day_together.navigation.AppDestinations


import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext


/**
 * 메시지 화면의 메인 컴포저블
 * 채팅 UI의 전체적인 레이아웃과 상태 관리
 * @param navController 화면 이동을 제어하는 NavController
 * @param modifier 외부에서 이 컴포저블에 적용할 Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // --- 상태 관리 변수들 ---
    var showSearchBar by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAttachmentOptions by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }

    val currentCalendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH)) }
    var selectedDisplayDate by remember { mutableStateOf<Int?>(null) }



    // 시스템 Intent를 사용하기 위한 현재 Context(앱의 상태 정보) 가져오기
    val context = LocalContext.current

    // 1. 앨범(갤러리)연동을 위한 ActivityResultLauncher 준비
    // PickVisualMedia : 최신 사진 선택기 API
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        // 사용자가 이미지를 성공적으로 선택했을 때 실행되는 부분
        if (uri != null) {
            Log.d("PhotoPicker", "Selected URI: $uri")
            // TODO: 선택된 이미지를 채팅방에 표시하거나 업로드하는 로직 추가
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }

    // 2. 파일 선택 연동을 위한 ActivityResultLauncher 준비
    // GetContent는 일반적인 파일 선택할 때 사용
    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // 사용자가 파일을 성공적으로 선택했을 때 실행되는 부분
        if (uri != null) {
            Log.d("FilePicker", "Selected URI: $uri")
            // TODO: 선택된 파일을 채팅방에 전송하는 로직 추가
        } else {
            Log.d("FilePicker", "No file selected")
        }
    }



    // (샘플 데이터) 날짜 선택창에 대화가 있는 날짜 표시하기 위한 Set
    val datesWithConversationsForPicker = remember(selectedYear, selectedMonth) {
        val monthAdjusted = selectedMonth + 1
        if (selectedYear == 2025 && monthAdjusted == 6) {
            setOf(LocalDate.of(2025, 6, 3), LocalDate.of(2025, 6, 9), LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 21), LocalDate.of(2025, 6, 27))
        } else if (selectedYear == LocalDate.now().year && monthAdjusted == LocalDate.now().monthValue) {
            setOf(LocalDate.now().minusDays(2), LocalDate.now().minusDays(5))
        } else {
            emptySet()
        }
    }

    // 화면의 전체적인 구조를 정의하는 Scaffold
    Scaffold(
        topBar = {
            MessageTopBar(
                showSearchBar = showSearchBar,
                searchText = searchText,
                onSearchTextChanged = { searchText = it },
                onToggleSearchBar = { showSearchBar = !showSearchBar; if(showSearchBar) showDatePicker = false },
                onCalendarClick = { showDatePicker = !showDatePicker; if(showDatePicker) showSearchBar = false },
                onMoreOptionsClick = {
                    navController.navigate(AppDestinations.CHAT_INFO_ROUTE)
                }
            )
        },
        containerColor = ScreenBackground
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PinnedChatbotMessageBubble()
                ChatMessagesList(modifier = Modifier.weight(1f), messages = sampleMessages)
                MessageInputArea(
                    text = messageText,
                    onTextChanged = { messageText = it },
                    onClipClick = { showAttachmentOptions = !showAttachmentOptions },
                    onSendClick = {
                        if (messageText.isNotBlank()) {
                            messageText = ""
                        }
                    }
                )
            }

            if (showDatePicker) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(onClick = { showDatePicker = false })
                ) {
                    MessageDatePicker(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 56.dp)
                            .padding(horizontal = 24.dp)
                            .clickable(enabled = true) {},
                        currentYear = selectedYear,
                        currentMonth = selectedMonth,
                        selectedDate = selectedDisplayDate,
                        datesWithConversations = datesWithConversationsForPicker,
                        onDateSelected = { year, month, day ->
                            selectedDisplayDate = day
                            showDatePicker = false
                        },
                        onMonthChange = { year, month ->
                            selectedYear = year
                            selectedMonth = month
                            selectedDisplayDate = null
                        },
                        onDismiss = { showDatePicker = false }
                    )
                }
            }

            // 첨부 파일 옵션이 활성화되면 화면에 표시
            if (showAttachmentOptions) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { showAttachmentOptions = false }
                ) {
                    // AttachmentOptionsPanel의 onClick 핸들러 수정
                    AttachmentOptionsPanel(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onDismiss = { showAttachmentOptions = false },
                        onAlbumClick = {
                            // 앨범 아이콘 클릭 시, 위에서 준비한 갤러리 런처 실행
                            // 이미지 타입만 선택하도록 요청
                            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            showAttachmentOptions = false // 패널 닫기
                        },
                        onCameraClick = {
                            // 카메라 앱을 직접 실행하는 Intent(명령) 생성
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            // 기기에 카메라 앱이 있는지 확인 후 실행
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "카메라 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                            showAttachmentOptions = false // 패널 닫기
                        },
                        onFileClick = {
                            // 파일 아이콘 클릭 시, 위에서 준비한 파일 선택 런처 실행
                            // "*/*"는 모든 종류의 파일 의미
                            pickFileLauncher.launch("*/*")
                            showAttachmentOptions = false // 패널 닫기
                        },
                        onVoiceMessageClick = {
                            // 음성 녹음기 앱을 실행하는 Intent 생성
                            val intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
                            // 기기에 음성 녹음기 앱이 있는지 확인 후 실행
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "음성 녹음 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                            showAttachmentOptions = false // 패널 닫기
                        }
                    )
                    // AttachmentOptionsPanel의 onClick 핸들러 수정
                }
            }
        }
    }
}

/**
 * 메시지 화면의 상단 앱 바 컴포저블
 * 일반 모드와 검색 모드를 전환하며 표시
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTopBar(
    showSearchBar: Boolean,
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    onToggleSearchBar: () -> Unit,
    onCalendarClick: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    TopAppBar(
        title = { }, // 타이틀 사용하지 않음
        actions = {
            // 검색창 모드일 때
            if (showSearchBar) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = onSearchTextChanged,
                    placeholder = { Text("내용 검색하기", style = TextStyle(fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.7f), fontFamily = GothicA1)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ScreenBackground.copy(alpha = 0.5f),
                        unfocusedContainerColor = ScreenBackground.copy(alpha = 0.5f),
                        disabledContainerColor = ScreenBackground.copy(alpha = 0.3f),
                        errorContainerColor = ScreenBackground.copy(alpha = 0.5f),
                        focusedBorderColor  = TextPrimary,
                        unfocusedBorderColor= NavIconUnselected,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        disabledTextColor = TextPrimary.copy(alpha = 0.5f),
                        cursorColor = TextPrimary
                    ),
                    textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontFamily = GothicA1),
                    singleLine = true
                )
                TextButton(onClick = {
                    onToggleSearchBar() // 검색창 닫기
                }) {
                    Text("확인", color = TextPrimary, style = TextStyle(fontSize = 14.sp, fontFamily = GothicA1, fontWeight = FontWeight.Medium))
                }
            } else { // 일반 모드일 때
                IconButton(onClick = onCalendarClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar),
                        contentDescription = "Calendar",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onToggleSearchBar) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onMoreOptionsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_options),
                        contentDescription = "More Options",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
    )
}

/**
 * 챗봇이 보낸 고정 메시지 표시하는 컴포저블
 */
@Composable
fun PinnedChatbotMessageBubble() {
    val chatbotMessage = MessageItem(
        id = 0,
        text = "오늘은 아빠의 생일! 오늘 뭐 할거야?",
        time = "19:05",
        isSentByMe = false,
        senderName = "챗봇"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_cloud_happy),
            contentDescription = chatbotMessage.senderName + " Avatar",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = chatbotMessage.senderName,
                style = TextStyle(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = GothicA1),
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            )
            Box(
                modifier = Modifier
                    .background(
                        color = AnniversaryBoardBackground.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.7f)
            ) {
                Text(
                    text = chatbotMessage.text,
                    style = TextStyle(fontSize = 14.sp, color = TextPrimary, fontFamily = GothicA1)
                )
            }
        }
        Text(
            text = chatbotMessage.time,
            style = TextStyle(fontSize = 10.sp, color = TextPrimary.copy(alpha = 0.7f), fontFamily = GothicA1),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

/**
 * todo : test용 데이터 삭제 필요
 * 메시지 한 개의 데이터를 담는 데이터 클래스
 */
data class MessageItem(val id: Int, val text: String, val time: String, val isSentByMe: Boolean, val senderName: String = "사용자")

/**
 * todo : test용 데이터 삭제 필요
 * 프리뷰 및 테스트를 위한 샘플 메시지 데이터
 */
val sampleMessages = listOf(
    MessageItem(1, "여보 생일 축하해 ~~ 저녁에 맛있는거 먹자!", "20:00", false, "챗봇 아님"),
    MessageItem(2, "아빠 생일 축하해~~", "20:05", true),
    MessageItem(3, "고마워! 저녁 기대할게!", "20:05", false, "챗봇 아님"),
    MessageItem(4, "케이크도 사갈까?", "20:05", true)
)

/**
 * 채팅 메시지 목록을 스크롤 가능한 리스트로 표시하는 컴포저블.
 */
@Composable
fun ChatMessagesList(modifier: Modifier = Modifier, messages: List<MessageItem>) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        reverseLayout = true,
        contentPadding = PaddingValues(top = 0.dp, bottom = 8.dp)
    ) {
        items(messages.reversed(), key = { it.id }) { message ->
            ChatMessageBubble(message)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * 개별 채팅 메시지 말풍선을 그리는 컴포저블
 * 내가 보낸 메시지와 상대방이 보낸 메시지 구분하여 다르게 표시
 */
@Composable
fun ChatMessageBubble(message: MessageItem) {
    val bubbleColor = if (message.isSentByMe) ButtonActiveBackground else AnniversaryBoardBackground.copy(alpha = 0.7f)
    val textColor = if (message.isSentByMe) ButtonActiveText else TextPrimary
    val timeColor = TextPrimary.copy(alpha = 0.7f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isSentByMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (message.isSentByMe) {
            Text(
                text = message.time,
                style = TextStyle(fontSize = 10.sp, color = timeColor, fontFamily = GothicA1),
                modifier = Modifier.padding(end = 6.dp)
            )
        }
        if (!message.isSentByMe) {
            Image(
                painter = painterResource(id = R.drawable.ic_add_photo),
                contentDescription = "${message.senderName} Avatar",
                modifier = Modifier.size(30.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column {
            if (!message.isSentByMe) {
                Text(
                    text = message.senderName,
                    style = TextStyle(fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium, fontFamily = GothicA1),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(
                        topStart = if (message.isSentByMe) 16.dp else 4.dp,
                        topEnd = if (message.isSentByMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    ))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.7f)
            ) {
                Text(text = message.text, style = TextStyle(fontSize = 14.sp, color = textColor, fontFamily = GothicA1))
            }
        }

        if (!message.isSentByMe) {
            Text(
                text = message.time,
                style = TextStyle(fontSize = 10.sp, color = timeColor, fontFamily = GothicA1),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * 화면 하단의 메시지 입력 영역 컴포저블.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInputArea(
    text: String,
    onTextChanged: (String) -> Unit,
    onClipClick: () -> Unit,
    onSendClick: () -> Unit
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ScreenBackground,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClipClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clip_attach),
                    contentDescription = "Attach file",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 32.dp, max = 64.dp),
                maxLines = 2,
                placeholder = { Text("메시지 입력", style = TextStyle(fontSize = 14.sp, color = NavIconUnselected, fontFamily = GothicA1)) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Brown100.copy(alpha = 0.5f),
                    unfocusedContainerColor = Brown100.copy(alpha = 0.5f),
                    disabledContainerColor = Brown100.copy(alpha = 0.3f),
                    errorContainerColor = Brown100.copy(alpha = 0.5f),
                    focusedBorderColor = TextPrimary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextPrimary.copy(alpha = 0.5f),
                    cursorColor = TextPrimary,
                ),
                textStyle = TextStyle(fontSize = 14.sp, color = TextPrimary, fontFamily = GothicA1)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onSendClick, enabled = text.isNotBlank()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_send_arrow),
                    contentDescription = "Send message",
                    tint = if (text.isNotBlank()) ButtonActiveBackground else NavIconUnselected,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 첨부 파일 옵션(앨범,카메라 등) 보여주는 패널 컴포저블
 */
@Composable
fun AttachmentOptionsPanel(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onAlbumClick: () -> Unit,
    onCameraClick: () -> Unit,
    onFileClick: () -> Unit,
    onVoiceMessageClick: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = ScreenBackground,
        border = BorderStroke(1.dp, WeeklyCalendarBorderColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttachmentOptionItem(iconResId = R.drawable.ic_gallery, label = "앨범", onClick = onAlbumClick)
                AttachmentOptionItem(iconResId = R.drawable.ic_camera, label = "카메라", onClick = onCameraClick)
                AttachmentOptionItem(iconResId = R.drawable.ic_file, label = "파일", onClick = onFileClick)
                AttachmentOptionItem(iconResId = R.drawable.ic_mic, label = "음성메시지", onClick = onVoiceMessageClick)
            }
        }
    }
}

/**
 * 첨부 파일 패널내부 각 아이템(아이콘+텍스트) 컴포저블
 */
@Composable
fun AttachmentOptionItem(iconResId: Int, label: String, onClick: () -> Unit) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AnniversaryBoardBackground.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconResId), contentDescription = label, tint = TextPrimary, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = TextStyle(fontSize = 12.sp, color = TextPrimary, fontFamily = GothicA1))
    }
}

/**
 * 메시지 기록을 날짜별로 찾아볼 수 있는 커스텀 달력 컴포저블
 */
@Composable
fun MessageDatePicker(
    modifier: Modifier = Modifier,
    currentYear: Int,
    currentMonth: Int,
    selectedDate: Int?,
    datesWithConversations: Set<LocalDate>,
    onDateSelected: (Int, Int, Int) -> Unit,
    onMonthChange: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {

    val calendar = remember { Calendar.getInstance() }
    LaunchedEffect(currentYear, currentMonth) {
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
    }

    val monthName = SimpleDateFormat("MMMM", Locale.KOREAN).format(calendar.time)
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeekInMonth = calendar.get(Calendar.DAY_OF_WEEK)
    val emptySlots = (firstDayOfWeekInMonth - Calendar.SUNDAY + 7) % 7

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    calendar.add(Calendar.MONTH, -1)
                    onMonthChange(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
                }) {
                    Icon(painterResource(id = R.drawable.ic_custom_arrow_left), "Previous Month", tint = TextPrimary)
                }
                Text("${currentYear}년 $monthName", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = GothicA1))
                IconButton(onClick = {
                    calendar.add(Calendar.MONTH, 1)
                    onMonthChange(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))
                }) {
                    Icon(painterResource(id = R.drawable.ic_custom_arrow_right), "Next Month", tint = TextPrimary)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { dayLabel ->
                    Text(dayLabel, style = TextStyle(fontSize = 12.sp, color = TextPrimary, fontFamily = GothicA1, textAlign = TextAlign.Center), modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            val totalSlots = daysInMonth + emptySlots
            val numRows = (totalSlots + 6) / 7

            for (week in 0 until numRows) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    for (dayPositionInWeek in 0 until 7) {
                        val dayIndex = week * 7 + dayPositionInWeek
                        if (dayIndex >= emptySlots && dayIndex < totalSlots) {
                            val day = dayIndex - emptySlots + 1
                            val isSelected = day == selectedDate

                            val currentDateBeingRendered = try {
                                LocalDate.of(currentYear, currentMonth + 1, day)
                            } catch (e: Exception) {
                                null
                            }
                            val hasConversation = currentDateBeingRendered != null && datesWithConversations.contains(currentDateBeingRendered)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> SelectedMonthlyBorder
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 0.dp else if (hasConversation) 1.5.dp else 0.dp,
                                        color = if (hasConversation && !isSelected) SelectedMonthlyBorder.copy(alpha=0.7f) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = currentDateBeingRendered != null) {
                                        currentDateBeingRendered?.let {
                                            onDateSelected(it.year, it.monthValue - 1, it.dayOfMonth)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontFamily = GothicA1,
                                        color = when {
                                            isSelected -> ScreenBackground
                                            hasConversation -> TextPrimary
                                            else -> OtherMonthDayText
                                        },
                                        fontWeight = if (isSelected || hasConversation) FontWeight.Medium else FontWeight.Normal
                                    )
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonActiveBackground),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("확인", color = ButtonActiveText, style = TextStyle(fontFamily = GothicA1, fontSize = 14.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}