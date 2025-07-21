package com.example.day_together.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    var isChatbotSettingsExpanded by remember { mutableStateOf(true) }


    var questionFrequency by remember { mutableStateOf("주3회") }
    val frequencyOptions = listOf("매일", "주3회", "주1회")


    var questionTime by remember { mutableStateOf("오후") }
    val timeOptions = listOf("오전", "오후", "저녁")


    var notificationEnabled by remember { mutableStateOf(true) }
    var vibrationEnabled by remember { mutableStateOf(false) }

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* 제목 없음 */ },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ScreenBackground,
                        navigationIconContentColor = TextPrimary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBackground)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 개인정보
                SettingSectionTitle(title = "개인정보")
                SettingClickableItem(title = "개인정보") {
                    navController.navigate(AppDestinations.EDIT_PROFILE_ROUTE)
                }
                ListDivider()


                SettingSectionTitle(title = "챗봇 설정")
                SettingRowTitle(title = "질문 빈도")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frequencyOptions.forEach { option ->
                        SelectableChipButton(
                            text = option,
                            selected = questionFrequency == option,
                            onClick = { questionFrequency = option }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                SettingRowTitle(title = "질문 시간대")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeOptions.forEach { option ->
                        SelectableChipButton(
                            text = option,
                            selected = questionTime == option,
                            onClick = { questionTime = option }
                        )
                    }
                }
                ListDivider(modifier = Modifier.padding(top = 16.dp))


                SettingSectionTitle(title = "시스템 설정")
                SwitchSettingItem(
                    title = "알림",
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = it }
                )
                SwitchSettingItem(
                    title = "진동",
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it }
                )
                SettingClickableItem(title = "언어 설정") {

                }
                ListDivider()


                SettingSectionTitle(title = "기타")
                SettingClickableItem(title = "시스템 버전 정보", showArrow = false) {  }
                SettingClickableItem(title = "이용약관") { }
                SettingClickableItem(title = "개인정보처리방침") { }
                SettingClickableItem(title = "문의하기") {  }
                ListDivider()


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { }) {
                        Text("로그아웃", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingRowTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingClickableItem(
    title: String,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        if (showArrow) {
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = "이동",
                tint = TextPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


@Composable
private fun SelectableChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(38.dp)
            .defaultMinSize(minWidth = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) ButtonActiveBackground.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (selected) TextPrimary else TextPrimary.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) ButtonActiveBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
    }
}


@Composable
private fun SwitchSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun ListDivider(modifier: Modifier = Modifier) {
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        thickness = 1.dp,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenPreview() {
    Day_togetherTheme {
        SettingsScreen(navController = rememberNavController())
    }
}