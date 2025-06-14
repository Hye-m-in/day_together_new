package com.example.day_together.ui.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class PhotoItem(val id: String, val imageUrl: String, val date: String)
data class MonthlyPhotoGroupData(val yearMonth: YearMonth, val photos: List<PhotoItem>)
data class MonthlyComment(val id: String = UUID.randomUUID().toString(), val author: String, val text: String, val timestamp: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(navController: NavController) {
    var currentDisplayYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val yearMonthFormatter = DateTimeFormatter.ofPattern("yyyy년 MM월", Locale.KOREAN)
    var showCommentBottomSheetFor by remember { mutableStateOf<YearMonth?>(null) }
    var showYearMonthPickerDialog by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    val samplePhotos = listOf(
        PhotoItem("s1", "https://picsum.photos/seed/202401/200/300", YearMonth.of(2024, 1).atDay(10).toString()),
        PhotoItem("s2", "https://picsum.photos/seed/202403/200/300", YearMonth.of(2024, 3).atDay(5).toString()),
        PhotoItem("s3", "https://picsum.photos/seed/current_prev/200/300", YearMonth.now().minusMonths(1).atDay(15).toString()),
        PhotoItem("s4", "https://picsum.photos/seed/current/200/300", YearMonth.now().atDay(1).toString()),
        PhotoItem("s5", "https://picsum.photos/seed/current_plus1/200/300", YearMonth.now().plusMonths(1).atDay(20).toString()),
        PhotoItem("s6", "https://picsum.photos/seed/current_plus2/200/300", YearMonth.now().plusMonths(2).atDay(10).toString()),
        PhotoItem("s7", "https://picsum.photos/seed/202512/200/300", YearMonth.of(2025,12).atDay(25).toString())
    )

    val allMonthlyPhotoGroups = remember(samplePhotos, currentDisplayYearMonth) {
        val photosByYearMonth = samplePhotos.groupBy { YearMonth.from(LocalDate.parse(it.date)) }
        val distinctYearMonthsInPhotos = photosByYearMonth.keys
        val allDisplayableYearMonths = (distinctYearMonthsInPhotos + currentDisplayYearMonth)
            .distinct()
            .sorted()
        allDisplayableYearMonths.map { ym ->
            MonthlyPhotoGroupData(
                yearMonth = ym,
                photos = photosByYearMonth[ym]?.sortedBy { photo -> LocalDate.parse(photo.date) } ?: emptyList()
            )
        }
    }

    LaunchedEffect(currentDisplayYearMonth, allMonthlyPhotoGroups) {
        if (allMonthlyPhotoGroups.isNotEmpty()) {
            val indexToScroll = allMonthlyPhotoGroups.indexOfFirst { it.yearMonth == currentDisplayYearMonth }
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
                        IconButton(onClick = { showYearMonthPickerDialog = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_year_month_picker),
                                contentDescription = "날짜 선택",
                                tint = TextPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ScreenBackground,
                        titleContentColor = TextPrimary
                    )
                )
            }
        ) { innerPadding ->
            if (samplePhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(ScreenBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "공유된 사진이 아직 없어요.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScreenBackground)
                        .padding(innerPadding)
                        .padding(horizontal = 8.dp)
                ) {
                    items(
                        items = allMonthlyPhotoGroups,
                        key = { it.yearMonth.toString() }
                    ) { group ->
                        MonthlyPhotoGroupItem(
                            yearMonth = group.yearMonth,
                            photos = group.photos,
                            yearMonthFormatter = yearMonthFormatter,
                            onPhotoClick = { },
                            onCommentIconClick = { yearMonth -> showCommentBottomSheetFor = yearMonth }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showYearMonthPickerDialog) {
                WheelCustomYearMonthPickerDialog(
                    initialYearMonth = currentDisplayYearMonth,
                    onDismissRequest = {
                        val finalSelection = WheelCustomYearMonthPickerDialogDefaults.getSelection()
                        currentDisplayYearMonth = finalSelection
                        showYearMonthPickerDialog = false
                    },
                    onConfirm = { selectedYearMonth ->
                        currentDisplayYearMonth = selectedYearMonth
                        showYearMonthPickerDialog = false
                    }
                )
            }

            showCommentBottomSheetFor?.let { ym ->
                MonthlyCommentBottomSheet(
                    yearMonth = ym,
                    onDismiss = { showCommentBottomSheetFor = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyCommentBottomSheet(
    yearMonth: YearMonth,
    onDismiss: () -> Unit
) {
    val comments = remember(yearMonth) {
        mutableStateListOf(
            MonthlyComment(author = "엄마", text = "행복했던 ${yearMonth.monthValue}월!", timestamp = "2시간 전"),
            MonthlyComment(author = "오빠", text = "이번 달이 벌써 끝나간다니.. 시간 너무 빠르다", timestamp = "2시간 전"),
            MonthlyComment(author = "아빠", text = "앞으로도 행복한 일만 가득하길~", timestamp = "1시간 전")
        )
    }
    var newCommentText by remember { mutableStateOf("") }

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
            color = ScreenBackground, // Surface 배경색은 기존 ModalBottomSheet의 containerColor 사용
            tonalElevation = 4.dp // 기존 ModalBottomSheet의 tonalElevation 사용
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
                        "아직 작성된 한마디가 없어요. 가장 먼저 첫 번째 한마디를 남겨보세요!",
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
                        onValueChange = { newCommentText = it },
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
                        keyboardActions = KeyboardActions(onSend = {
                            if (newCommentText.isNotBlank()) {
                                comments.add(0, MonthlyComment(author = "나", text = newCommentText, timestamp = "방금 전"))
                                newCommentText = ""
                            }
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = ButtonActiveBackground,
                            unfocusedIndicatorColor = TextPrimary.copy(alpha = 0.3f),
                            focusedLabelColor = TextPrimary,
                            cursorColor = ButtonActiveBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary.copy(alpha = 0.8f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                comments.add(0, MonthlyComment(author = "나", text = newCommentText, timestamp = "방금 전"))
                                newCommentText = ""
                            }
                        },
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
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            PhotoGridItem(photoItem = photo, onClick = { onPhotoClick(photo.id) })
        }
    }
}

internal object WheelCustomYearMonthPickerDialogDefaults {
    var selectedYear: Int = YearMonth.now().year
    var selectedMonthValue: Int = YearMonth.now().monthValue
    fun getSelection(): YearMonth = YearMonth.of(selectedYear, selectedMonthValue)
}


@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun GalleryScreenPreview() {
    Day_togetherTheme {
        GalleryScreen(navController = rememberNavController())
    }
}