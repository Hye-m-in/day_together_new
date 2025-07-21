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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.ui.theme.Day_togetherTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelCustomYearMonthPickerDialog(
    initialYearMonth: YearMonth,
    onDismissRequest: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
    yearRange: IntRange = (1900..2100)
) {
    val density = LocalDensity.current
    val itemHeightDp = 40.dp
    val itemHeightPx = density.run { itemHeightDp.toPx() }
    val threshold = remember { itemHeightPx / 2f }

    val yearListState = rememberLazyListState()
    val monthListState = rememberLazyListState()

    var userHasInteracted by remember { mutableStateOf(false) }

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

    LaunchedEffect(yearListState, userHasInteracted) {
        snapshotFlow { yearListState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it && userHasInteracted }
            .collect {
                val currentSelection = YearMonth.of(selectedYear, selectedMonthValue)
                onConfirm(currentSelection)
            }
    }

    LaunchedEffect(monthListState, userHasInteracted) {
        snapshotFlow { monthListState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it && userHasInteracted }
            .collect {
                val currentSelection = YearMonth.of(selectedYear, selectedMonthValue)
                onConfirm(currentSelection)
            }
    }

    LaunchedEffect(Unit) {
        val targetYearIndex = (initialYearMonth.year - yearRange.first).coerceIn(0, yearRange.count() - 1)
        yearListState.scrollToItem(targetYearIndex)
        val targetMonthIndex = (initialYearMonth.monthValue - 1).coerceIn(0, 11)
        monthListState.scrollToItem(targetMonthIndex)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = null,
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyColumn(
                        state = yearListState,
                        contentPadding = PaddingValues(vertical = 80.dp),
                        flingBehavior = rememberSnapFlingBehavior(yearListState),
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope { // This scope provides awaitPointerEvent
                                    awaitFirstDown(requireUnconsumed = false) // Detect first touch
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemHeightDp)
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

                    LazyColumn(
                        state = monthListState,
                        contentPadding = PaddingValues(vertical = 80.dp),
                        flingBehavior = rememberSnapFlingBehavior(monthListState),
                        modifier = Modifier
                            .weight(1f)
                            .height(200.dp)
                            .pointerInput(Unit) {
                                awaitPointerEventScope { // This scope provides awaitPointerEvent
                                    awaitFirstDown(requireUnconsumed = false) // Detect first touch
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(itemHeightDp)
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
        confirmButton = {},
        dismissButton = {}
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
fun WheelCustomYearMonthPickerDialogPreview() {
    Day_togetherTheme {
        Surface {
            WheelCustomYearMonthPickerDialog(
                initialYearMonth = YearMonth.of(2025, 5),
                onDismissRequest = { },
                onConfirm = { }
            )
        }
    }
}