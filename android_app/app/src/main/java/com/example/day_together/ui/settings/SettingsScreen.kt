package com.example.day_together.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.*

/**
 * 설정 화면의 UI를 구성하고 사용자의 입력을 ViewModel로 전달하는 메인 컴포저블
 * @param appNavController 앱의 최상위 NavController.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appNavController: NavController
) {
    // 1. viewModel() 함수를 통해 SettingsViewModel 인스턴스를 가져옴
    val viewModel: SettingsViewModel = viewModel()
    // 2. ViewModel의 StateFlow를 구독하고 변경될 때마다 uiState가 자동으로 업데이트
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 3. ViewModel로부터 오는 일회성 이벤트(로그아웃 후 화면 전환 등)를 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvent.NavigateToLogin -> {
                    // 로그인 화면으로 이동하고 이전 화면 스택을 모두 제거하여 뒤로가기 방지
                    appNavController.navigate(AppDestinations.LOGIN_ROUTE) {
                        popUpTo(appNavController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }

    // UI에 표시될 옵션 리스트
    val frequencyOptions = listOf("매일", "주3회", "주1회")
    val timeOptions = listOf("오전", "오후", "저녁")

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("설정", style = MaterialTheme.typography.titleLarge, color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ScreenBackground
                    )
                )
            }
        ) { innerPadding ->
            // 4. ViewModel의 로딩 상태에 따라 UI 분기
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 5. 로딩이 완료되면 uiState를 사용하여 실제 설정 화면을 그림
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScreenBackground)
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 개인정보 섹션
                    SettingSectionTitle(title = "개인정보")
                    SettingClickableItem(title = "개인정보 수정") {
                        appNavController.navigate(AppDestinations.EDIT_PROFILE_ROUTE)
                    }
                    ListDivider()

                    // 챗봇 설정 섹션
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
                                selected = uiState.questionFrequency == option,
                                onClick = { viewModel.onFrequencyChange(option) } // 클릭 이벤트를 ViewModel로 전달
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
                                selected = uiState.questionTime == option,
                                onClick = { viewModel.onTimeChange(option) } // 클릭 이벤트를 ViewModel로 전달
                            )
                        }
                    }
                    ListDivider(modifier = Modifier.padding(top = 16.dp))

                    // --- 시스템 설정 섹션 ---
                    SettingSectionTitle(title = "시스템 설정")
                    SettingClickableItem(title = "알림 설정") {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                    SettingClickableItem(title = "언어 설정") {
                        val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                        context.startActivity(intent)
                    }
                    ListDivider()

                    // 기타 섹션
                    SettingSectionTitle(title = "기타")
                    SettingClickableItem(title = "시스템 버전 정보", showArrow = false) { /* TODO */ }
                    SettingClickableItem(title = "이용약관") { /* TODO */ }
                    SettingClickableItem(title = "개인정보처리방침") { /* TODO */ }
                    SettingClickableItem(title = "문의하기") { /* TODO */ }
                    ListDivider()

                    // 로그아웃 버튼
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 버튼 클릭 시 ViewModel의 onLogoutClicked 함수 호출
                        TextButton(onClick = { viewModel.onLogoutClicked() }) {
                            Text("로그아웃", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 설정 화면의 각 구획을 나누는 제목
 */
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

/**
 * 설정 항목의 제목
 */
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

/**
 * 클릭 가능한 설정 항목
 */
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

/**
 * 선택 가능한 칩 형태 버튼
 */
@Composable
private fun SelectableChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
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

/**
 * 설정 항목들을 시각적으로 구분하는 구분선
 */
@Composable
private fun ListDivider(modifier: Modifier = Modifier) {
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        thickness = 1.dp,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    )
}