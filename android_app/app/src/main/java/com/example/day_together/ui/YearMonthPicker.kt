package com.example.day_together.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.YearMonth

/**
 * 년/월을 선택하는 커스텀 다이얼로그 Composable
 *
 * @param initialYearMonth 다이얼로그가 처음 열렸을 때 표시될 초기 년/월
 * @param onDismissRequest 다이얼로그가 닫힐 때(외부 클릭, 뒤로가기 등) 호출될 콜백
 * @param onSelectionChanged 사용자가 스크롤을 멈춰서 새로운 년/월이 선택될 때마다 호출될 콜백
 * @param yearRange 선택 가능한 년도의 범위
 */
@Composable
fun WheelCustomYearMonthPickerDialog(
    initialYearMonth: YearMonth,
    onDismissRequest: () -> Unit,
    onSelectionChanged: (YearMonth) -> Unit,
    yearRange: IntRange = (1900..2100)
) {
    // 1. 초기 설정 및 계산
    val density = LocalDensity.current
    val itemHeightDp = 40.dp
    val itemHeightPx = density.run { itemHeightDp.toPx() }
    val threshold = remember { itemHeightPx / 2f }

    // 2. 스크롤 상태 관리
    val yearListState = rememberLazyListState()
    val monthListState = rememberLazyListState()
    var userHasInteracted by remember { mutableStateOf(false) }

    // 3. 현재 선택된 년/월 계산
    val selectedYear by remember {
        derivedStateOf {
            if (yearListState.layoutInfo.visibleItemsInfo.isEmpty() && !userHasInteracted) {
                initialYearMonth.year
            } else {
                val firstVisibleYearIndex = yearListState.firstVisibleItemIndex
                val scrollOffset = yearListState.firstVisibleItemScrollOffset
                yearRange.first + firstVisibleYearIndex + if (scrollOffset >= threshold) 1 else 0
            }
        }
    }

    val selectedMonthValue by remember {
        derivedStateOf {
            if (monthListState.layoutInfo.visibleItemsInfo.isEmpty() && !userHasInteracted) {
                initialYearMonth.monthValue
            } else {
                val firstVisibleMonthIndex = monthListState.firstVisibleItemIndex
                val scrollOffset = monthListState.firstVisibleItemScrollOffset
                firstVisibleMonthIndex + (if (scrollOffset >= threshold) 1 else 0) + 1
            }
        }
    }

    // 4. Side-Effects 처리
    // 스크롤이 멈추면 선택 변경만 알리도록
    LaunchedEffect(yearListState, userHasInteracted) {
        snapshotFlow { yearListState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it && userHasInteracted }
            .collect {
                val currentSelection = YearMonth.of(selectedYear, selectedMonthValue)
                onSelectionChanged(currentSelection) // 외부로 변경된 값만 전달
            }
    }

    LaunchedEffect(monthListState, userHasInteracted) {
        snapshotFlow { monthListState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it && userHasInteracted }
            .collect {
                val currentSelection = YearMonth.of(selectedYear, selectedMonthValue)
                onSelectionChanged(currentSelection) // 외부로 변경된 값만 전달
            }
    }


    // 다이얼로그가 처음 생성될 때 한 번만 실행
    LaunchedEffect(Unit) {
        val targetYearIndex = (initialYearMonth.year - yearRange.first).coerceIn(0, yearRange.count() - 1)
        yearListState.scrollToItem(targetYearIndex)
        val targetMonthIndex = (initialYearMonth.monthValue - 1).coerceIn(0, 11)
        monthListState.scrollToItem(targetMonthIndex)
    }

    // 5. UI AlertDialog 구성
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 년(Year) 선택 LazyColumn
                    LazyColumn(
                        state = yearListState,
                        contentPadding = PaddingValues(vertical = 80.dp),
                        flingBehavior = rememberSnapFlingBehavior(yearListState),
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown(requireUnconsumed = false)
                                    userHasInteracted = true
                                }
                            }
                    ) {
                        items(yearRange.count()) { index ->
                            val year = yearRange.first + index
                            val textColor by animateColorAsState(
                                targetValue = if (year == selectedYear) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                label = "yearTextColor"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().height(itemHeightDp)
                            ) {
                                BasicText(
                                    text = "${year}년",
                                    style = LocalTextStyle.current.copy(
                                        fontSize = 21.sp,
                                        fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                )
                            }
                        }
                    }

                    // 월 선택
                    LazyColumn(
                        state = monthListState,
                        contentPadding = PaddingValues(vertical = 80.dp),
                        flingBehavior = rememberSnapFlingBehavior(monthListState),
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    awaitFirstDown(requireUnconsumed = false)
                                    userHasInteracted = true
                                }
                            }
                    ) {
                        items(12) { index ->
                            val month = index + 1
                            val textColor by animateColorAsState(
                                targetValue = if (month == selectedMonthValue) MaterialTheme.colorScheme.onSurface else Color.Gray,
                                label = "monthTextColor"
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth().height(itemHeightDp)
                            ) {
                                BasicText(
                                    text = "${month}월",
                                    style = LocalTextStyle.current.copy(
                                        fontSize = 21.sp,
                                        fontWeight = if (month == selectedMonthValue) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        // 버튼이 없는 디자인이므로 confirm/dismiss 버튼은 비워둠
        confirmButton = {},
        dismissButton = {}
    )
}