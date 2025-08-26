package com.example.day_together.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.day_together.R
import com.example.day_together.ui.theme.ButtonActiveBackground
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.ScreenBackground
import com.example.day_together.ui.theme.TextPrimary
import com.example.day_together.ui.theme.WeeklyCalendarBorderColor
import com.example.day_together.ui.WheelCustomYearMonthPickerDialog
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID


// 데이터 클래스 정의
data class PhotoItem(val id: String, val imageUrl: String, val date: String)
data class MonthlyPhotoGroupData(val yearMonth: YearMonth, val photos: List<PhotoItem>)
data class MonthlyComment(val id: String = UUID.randomUUID().toString(), val author: String, val text: String, val timestamp: String)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: GalleryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val yearMonthFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN) }
    var showYearMonthPickerDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    // 다이얼로그에서 선택된 값을 임시로 저장할 상태 변수
    var tempSelectedYearMonth by remember { mutableStateOf(uiState.currentDisplayYearMonth) }


    LaunchedEffect(uiState.currentDisplayYearMonth, uiState.allMonthlyPhotoGroups) {
        if (uiState.allMonthlyPhotoGroups.isNotEmpty()) {
            val indexToScroll = uiState.allMonthlyPhotoGroups.indexOfFirst { it.yearMonth == uiState.currentDisplayYearMonth }
            if (indexToScroll != -1) {
                lazyListState.animateScrollToItem(indexToScroll)
            }
        }
    }

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        IconButton(onClick = {
                            // 다이얼로그를 열 때, 임시상태를 현재 ViewModel의 상태와 동기화
                            tempSelectedYearMonth = uiState.currentDisplayYearMonth
                            showYearMonthPickerDialog = true
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_year_month_picker),
                                contentDescription = "날짜 선택",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
                )
            }
        ) { innerPadding ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.allMonthlyPhotoGroups.all { it.photos.isEmpty() }) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("공유된 사진이 아직 없어요.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 8.dp)
                ) {
                    items(
                        items = uiState.allMonthlyPhotoGroups,
                        key = { it.yearMonth.toString() }
                    ) { group ->
                        MonthlyPhotoGroupItem(
                            yearMonth = group.yearMonth,
                            photos = group.photos,
                            yearMonthFormatter = yearMonthFormatter,
                            onPhotoClick = { /* ... */ },
                            onCommentIconClick = { viewModel.onCommentIconClicked(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showYearMonthPickerDialog) {
                //다이얼로그 호출 로직
                WheelCustomYearMonthPickerDialog(
                    initialYearMonth = uiState.currentDisplayYearMonth,
                    // 다이얼로그가 닫힐 때(외부 클릭,뒤로가기)
                    onDismissRequest = {
                        // 다이얼로그가 닫히는 시점의 임시 값을 최종 값으로 확정
                        viewModel.onYearMonthSelected(tempSelectedYearMonth)
                        showYearMonthPickerDialog = false
                    },
                    // 스크롤이 멈출 때마다 호출됨
                    onSelectionChanged = { newSelection ->
                        // ViewModel의 상태를 직접 바꾸지 않고, 임시 상태만 업데이트
                        tempSelectedYearMonth = newSelection
                    }
                )

            }

            uiState.commentSheetYearMonth?.let { ym ->
                MonthlyCommentBottomSheet(
                    yearMonth = ym,
                    comments = uiState.comments,
                    newCommentText = uiState.newCommentText,
                    onNewCommentChange = viewModel::onNewCommentChange,
                    onSendComment = viewModel::onSendComment,
                    onDismiss = viewModel::onCommentSheetDismissed
                )
            }
        }
    }
}




// 월별 댓글 목록(댓글 BottomSheet)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCommentBottomSheet(
    yearMonth: YearMonth,
    comments: List<MonthlyComment>,
    newCommentText: String,
    onNewCommentChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, WeeklyCalendarBorderColor.copy(alpha = 0.5f)),
            color = ScreenBackground,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .imePadding()
            ) {
                Text(
                    text = "${yearMonth.format(DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN))}의 한마디",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
                )

                if (comments.isEmpty()) {
                    Text(
                        "아직 작성된 한마디가 없어요.\n가장 먼저 첫 번째 한마디를 남겨보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .heightIn(max = 200.dp)
                    ) {
                        items(comments.size, key = { comments[it].id }) { index ->
                            val comment = comments[index]
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(comment.author, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, color = TextPrimary))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(comment.timestamp, style = MaterialTheme.typography.labelSmall, color = TextPrimary.copy(alpha = 0.6f))
                                }
                                Text(comment.text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            }
                            if (index < comments.lastIndex) {
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = onNewCommentChange,
                        placeholder = {
                            Text(
                                "이번 달 우리 가족에게 남기는 한마디",
                                fontSize = 13.sp,
                                color = TextPrimary.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendComment() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = ButtonActiveBackground,
                            unfocusedIndicatorColor = TextPrimary.copy(alpha = 0.3f),
                            cursorColor = ButtonActiveBackground,
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onSendComment() },
                        enabled = newCommentText.isNotBlank()
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_send_arrow),
                            contentDescription = "댓글 전송",
                            tint = if (newCommentText.isNotBlank()) ButtonActiveBackground else TextPrimary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

// 한 달 단위의 사진그룹(제목+사진 그리드) 표시
@Composable
fun MonthlyPhotoGroupItem(
    yearMonth: YearMonth,
    photos: List<PhotoItem>,
    yearMonthFormatter: DateTimeFormatter,
    onPhotoClick: (photoId: String) -> Unit,
    onCommentIconClick: (yearMonth: YearMonth) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = yearMonth.format(yearMonthFormatter),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onCommentIconClick(yearMonth) }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "${yearMonth.format(yearMonthFormatter)} 댓글 보기",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (photos.isEmpty()) {
            Text(
                text = "해당 월에는 사진이 존재하지 않습니다.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.7f)
            )
        } else {
            PhotoGrid(photos = photos, onPhotoClick = onPhotoClick)
        }
    }
}

// 3열 그리드 형태로 사진 표시
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoGrid(photos: List<PhotoItem>, onPhotoClick: (photoId: String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(((photos.size + 2) / 3 * 130).dp),
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoGridItem(photoItem = photo, onClick = { onPhotoClick(photo.id) })
        }
    }
}

// 그리드에 들어가는 개별 사진 아이템 Composable
@Composable
fun PhotoGridItem(photoItem: PhotoItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoItem.imageUrl)
                .crossfade(true)
                .error(R.drawable.ic_placeholder_image)
                .placeholder(R.drawable.ic_placeholder_image)
                .build(),
            contentDescription = "갤러리 사진 ${photoItem.id}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun GalleryScreenPreview() {
    Day_togetherTheme {
        GalleryScreen(navController = rememberNavController())
    }
}