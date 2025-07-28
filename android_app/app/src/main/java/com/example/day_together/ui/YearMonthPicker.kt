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
import androidx.compose.material3.ExperimentalMaterial3Api
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
 * 타임피커 -> LazyColumn, SnapFlingBehavior 사용
 *
 * @param initialYearMonth -> 다이얼로그 처음 열렸을 때 표시될 초기 년/월
 * @param onDismissRequest 다이얼로그가 닫힐 때(외부 클릭, 뒤로가기 등) 호출될 콜백
 * @param onConfirm 사용자가 스크롤을 멈춰서 새로운 년/월이 선택할 때마다 호출될 콜백
 * @param yearRange 선택 가능한 년도 범위
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelCustomYearMonthPickerDialog(
    initialYearMonth: YearMonth,
    onDismissRequest: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
    yearRange: IntRange = (1900..2100)
) {
    // 1. 초기 설정 및 계산
    val density = LocalDensity.current
    val itemHeightDp = 40.dp
    val itemHeightPx = density.run { itemHeightDp.toPx() } // 아이템높이를 Px 단위로
    // 아이템이 중앙에 위치했는지 판단(아이템높이 절반)
    val threshold = remember { itemHeightPx / 2f }

    // 2. 스크롤 상태 관리
    val yearListState = rememberLazyListState()
    val monthListState = rememberLazyListState()
    // 사용자가 스크롤을 시작했는지 확인
    var userHasInteracted by remember { mutableStateOf(false) }

    // 3. 현재 선택된 년/월 계산
    // derivedStateOf: 스크롤 위치 변경될 때만 재계산
    val selectedYear by remember {
        derivedStateOf {
            // 초기 상태 or 사용자가 아직 스크롤하지 안했으면 초기 년도 반환
            if (yearListState.layoutInfo.visibleItemsInfo.isEmpty() && !userHasInteracted) {
                initialYearMonth.year
            } else {
                // 스크롤 위치 바탕으로 중앙에 위치한 년도 계산
                val firstVisibleYearIndex = yearListState.firstVisibleItemIndex
                val scrollOffset = yearListState.firstVisibleItemScrollOffset
                // 시작년도 + 첫번째 보이는 아이템인덱스
                // 스크롤이 절반 이상인 경우 1, 아니면 0)
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
                // (첫번째로 보이는 아이템인덱스) + (스크롤 절반이상 여부) + 1(인덱스 0부터 시작)
                firstVisibleMonthIndex + (if (scrollOffset >= threshold) 1 else 0) + 1
            }
        }
    }

    // 4. Side-Effects 처리
    // 스크롤이 멈출 때마다 onConfirm 콜백을 호출하는 로직
    LaunchedEffect(yearListState, userHasInteracted) {
        // snapshotFlow: Composable의 상태(isScrollInProgress)를 Flow로 변환하여 관찰합니다.
        snapshotFlow { yearListState.isScrollInProgress }
            .distinctUntilChanged() // 스크롤 상태가 실제로 변경될 때만(true -> false) 이벤트 받음
            .filter { !it && userHasInteracted } // 스크롤이 멈춤 & 사용자가 상호작용한 경우에만 필터링
            .collect { // 필터링된 이벤트 수신하여 처리
                val currentSelection = YearMonth.of(selectedYear, selectedMonthValue)
                onConfirm(currentSelection)
            }
    }

    // 월(Month)리스트에도 동일한 로직 적용
    LaunchedEffect(monthListState, userHasInteracted) {
        snapshotFlow { monthListState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it && userHasInteracted }
            .collect {
                val currentSelection = YearMonth.of(selectedYear, selectedMonthValue)
                onConfirm(currentSelection)
            }
    }

    // 다이얼로그가 처음 생성될 때 한 번만 실행
    LaunchedEffect(Unit) {
        // 초기 년/월 값으로 스크롤 위치를 설정
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
                        contentPadding = PaddingValues(vertical = 80.dp), // 아이템이 중앙에 올 수 있게 함
                        flingBehavior = rememberSnapFlingBehavior(yearListState), // 스크롤 시 아이템에 딱 맞게 멈추도록 함
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .pointerInput(Unit) { // 사용자 입력 감지
                                awaitPointerEventScope {
                                    awaitFirstDown(requireUnconsumed = false) // 첫 터치 감지
                                    userHasInteracted = true // 사용자가 상호작용했음으로 기록
                                }
                            }
                    ) {
                        items(yearRange.count()) { index ->
                            val year = yearRange.first + index
                            // 선택된 아이템의 색상을 변경하는 애니메이션
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
        // 스크롤 멈춤(onConfirm), 외부 클릭(onDismissRequest)으로 동작
        confirmButton = {},
        dismissButton = {}
    )
}

