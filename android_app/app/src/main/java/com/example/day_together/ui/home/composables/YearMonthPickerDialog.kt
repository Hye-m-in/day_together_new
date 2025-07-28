package com.example.day_together.ui.home.composables

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.time.YearMonth
import kotlin.math.abs


@Composable
fun YearMonthPickerDialog(
    initialYearMonth: YearMonth,
    onDismissAndConfirm: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedYearState by remember { mutableStateOf(initialYearMonth.year) }
    var selectedMonthState by remember { mutableStateOf(initialYearMonth.monthValue) }
    val currentActualYear = YearMonth.now().year

    val yearRange = (currentActualYear - 70..currentActualYear + 30).toList()
    val monthRange = (1..12).toList()

    Dialog(
        onDismissRequest = {
            onDismissAndConfirm(YearMonth.of(selectedYearState, selectedMonthState))
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White, // 흰색 배경
            modifier = modifier
                .fillMaxWidth(0.75f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = yearRange.map { "${it}년" },
                        initialSelectedItemIndex = yearRange.indexOf(selectedYearState).coerceAtLeast(0),
                        onItemSelected = { index -> selectedYearState = yearRange[index] },
                        modifier = Modifier.weight(1.2f),
                        itemHeight = 36.dp,
                        visibleItemsCount = 5
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    WheelPicker(
                        items = monthRange.map { String.format("%02d월", it) },
                        initialSelectedItemIndex = monthRange.indexOf(selectedMonthState).coerceAtLeast(0),
                        onItemSelected = { index -> selectedMonthState = monthRange[index] },
                        modifier = Modifier.weight(0.8f),
                        itemHeight = 36.dp,
                        visibleItemsCount = 5
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    items: List<String>,
    initialSelectedItemIndex: Int,
    onItemSelected: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 36.dp,
    visibleItemsCount: Int = 5
) {
    val pickerTotalHeight = itemHeight * visibleItemsCount
    val extendedListCountFactor = 100
    val centralizingInitialIndex = (items.size * extendedListCountFactor / 2) - (visibleItemsCount / 2) + initialSelectedItemIndex

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(initialSelectedItemIndex, items.size, visibleItemsCount) {
        if (items.isNotEmpty() && initialSelectedItemIndex >= 0 && initialSelectedItemIndex < items.size) {
            val targetExtendedIndex = (items.size * extendedListCountFactor / 2) - (visibleItemsCount / 2) + initialSelectedItemIndex
            listState.scrollToItem(targetExtendedIndex.coerceAtLeast(0))

        }
    }


    LaunchedEffect(listState.isScrollInProgress, items) {
        if (!listState.isScrollInProgress && listState.layoutInfo.visibleItemsInfo.isNotEmpty() && items.isNotEmpty()) {
            val layoutInfo = listState.layoutInfo
            val viewportCenterY = layoutInfo.viewportSize.height / 2f

            val closestItemInfo = layoutInfo.visibleItemsInfo.minByOrNull { itemInfo ->
                val itemCenterInViewport = itemInfo.offset.toFloat() + itemInfo.size.toFloat() / 2f
                abs(itemCenterInViewport - viewportCenterY)
            }

            closestItemInfo?.let {
                val snappedIndexInExtendedList = it.index
                val actualItemIndex = snappedIndexInExtendedList % items.size
                val itemCenterInViewport = it.offset.toFloat() + it.size.toFloat() / 2f
                val scrollTo = itemCenterInViewport - viewportCenterY

                if (abs(scrollTo) > 0.5f) {
                    coroutineScope.launch { listState.animateScrollBy(scrollTo) }
                }
                if (actualItemIndex < items.size) {
                    onItemSelected(actualItemIndex)
                }
            }
        }
    }

    Box(
        modifier = modifier.height(pickerTotalHeight),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = (pickerTotalHeight - itemHeight) / 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(items.size * extendedListCountFactor) { i ->
                val actualIndex = i % items.size
                val itemText = items[actualIndex]

                val currentItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == i }
                val itemOffsetFromViewportCenter = currentItemInfo?.let {
                    (it.offset.toFloat() + it.size.toFloat() / 2f) - (listState.layoutInfo.viewportSize.height.toFloat() / 2f)
                } ?: Float.MAX_VALUE

                val normalizedDistance = abs(itemOffsetFromViewportCenter) / itemHeight.value.toFloat()
                val scale = (1f - (normalizedDistance * 0.25f)).coerceIn(0.7f, 1f)
                val alpha = (1f - (normalizedDistance * 0.45f)).coerceIn(0.35f, 1f)


                val isSelected = abs(itemOffsetFromViewportCenter) < (itemHeight.value * 0.45f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = alpha,
                            transformOrigin = TransformOrigin.Center
                        )
                        .clickable {
                            coroutineScope.launch {
                                val targetExtendedIndexToSnap = (items.size * extendedListCountFactor / 2) - (visibleItemsCount / 2) + actualIndex
                                listState.animateScrollToItem(targetExtendedIndexToSnap.coerceAtLeast(0))

                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemText,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = if (isSelected) 20.sp else 17.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else Color.Gray.copy(alpha = alpha.coerceAtLeast(0.5f))
                        )
                    )
                }
            }
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight + 4.dp)
                .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                .align(Alignment.Center)
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center).offset(y = -itemHeight / 2 - 2.dp),
            thickness = 1.dp,
            color = Color.Black.copy(alpha = 0.12f)
        )
        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center).offset(y = itemHeight / 2 + 2.dp),
            thickness = 1.dp,
            color = Color.Black.copy(alpha = 0.12f)
        )
    }
}
