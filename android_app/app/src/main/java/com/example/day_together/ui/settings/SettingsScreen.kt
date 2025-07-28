package com.example.day_together.ui.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // --- ViewModel 및 상태 설정 ---
    // 1. ViewModel 인스턴스를 가져오고, UI 상태 구독
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    // Intent를 사용하여 시스템 설정 화면을 열기 위해 현재 Context를 가져옴
    val context = LocalContext.current

    // 2. ViewModel로부터 오는 일회성 이벤트(로그아웃 후 화면 전환 등) 처리
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvent.NavigateToLogin -> {
                    // 로그인 화면으로 이동하고 이전 화면 스택을 모두 제거하여 뒤로가기 방지
                    navController.navigate(AppDestinations.ONBOARDING_ROUTE) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            }
        }
    }

    // --- UI 옵션 리스트 ---
    val frequencyOptions = listOf("매일", "주3회", "주1회")
    val timeOptions = listOf("오전", "오후", "저녁")

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
            // 3. ViewModel의 로딩 상태에 따라 UI 분기
            if (uiState.isLoading) {
                // 설정 값을 불러오는 동안 로딩 인디케이터 표시
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 4. 로딩 완료 후, ViewModel의 uiState를 사용
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ScreenBackground)
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- 개인정보 섹션 ---
                    SettingSectionTitle(title = "개인정보")
                    SettingClickableItem(title = "개인정보 수정") {
                        navController.navigate(AppDestinations.EDIT_PROFILE_ROUTE)
                    }
                    ListDivider()

                    // --- 챗봇 설정 섹션 ---
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
                                selected = uiState.questionFrequency == option, // ViewModel의 상태를 사용
                                onClick = { viewModel.onFrequencyChange(option) } // ViewModel의 함수를 호출
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
                                selected = uiState.questionTime == option, // ViewModel의 상태를 사용
                                onClick = { viewModel.onTimeChange(option) } // ViewModel의 함수를 호출
                            )
                        }
                    }
                    ListDivider(modifier = Modifier.padding(top = 16.dp))

                    // --- 시스템 설정 섹션 ---
                    SettingSectionTitle(title = "시스템 설정")
                    // '알림 설정' 클릭 시 안드로이드의 앱 알림 설정 화면으로 이동
                    SettingClickableItem(title = "알림 설정") {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                    // '언어 설정' 클릭 시 안드로이드의 언어 설정 화면으로 이동
                    SettingClickableItem(title = "언어 설정") {
                        val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
                        context.startActivity(intent)
                    }
                    ListDivider()

                    // --- 기타 섹션 ---
                    SettingSectionTitle(title = "기타")
                    SettingClickableItem(title = "시스템 버전 정보", showArrow = false) { /* TODO */ }
                    SettingClickableItem(title = "이용약관") { /* TODO */ }
                    SettingClickableItem(title = "개인정보처리방침") { /* TODO */ }
                    SettingClickableItem(title = "문의하기") { /* TODO */ }
                    ListDivider()

                    // --- 로그아웃 버튼 ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { viewModel.onLogoutClicked() }) { // ViewModel의 로그아웃 함수 호출
                            Text("로그아웃", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}


/**
 * 설정 화면의 각 구획을 나누는 회색 제목 텍스트를 표시
 * 예: "개인정보", "챗봇 설정"
 *
 * @param title 표시할 제목 텍스트.
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
 * 설정 항목의 제목을 표시 (예: "질문 빈도", "질문 시간대")
 *
 * @param title 표시할 제목 텍스트.
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
 * 클릭 가능한 설정 항목을 만듭니다. 주로 화면 이동에 사용됩니다.
 *
 * @param title 항목의 제목.
 * @param showArrow 오른쪽에 화살표 아이콘을 표시할지 여부.
 * @param onClick 항목을 클릭했을 때 실행될 동작.
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
 * 선택 가능한 칩(Chip) 형태의 버튼입니다.
 * 여러 옵션 중 하나를 선택하는 UI에 사용됩니다.
 *
 * @param text 버튼에 표시될 텍스트.
 * @param selected 이 버튼이 현재 선택된 상태인지 여부.
 * @param onClick 버튼을 클릭했을 때 실행될 동작.
 * @param modifier 추가적으로 적용할 Modifier.
 */
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
            // 선택 상태에 따라 배경 및 글자 색상 변경
            containerColor = if (selected) ButtonActiveBackground.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (selected) TextPrimary else TextPrimary.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            width = 1.dp,
            // 선택 상태에 따라 테두리 색상 변경
            color = if (selected) ButtonActiveBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
    }
}


/**
 * 제목과 스위치(Switch)로 구성된 설정 항목입니다.
 *
 * @param title 항목의 제목.
 * @param checked 스위치의 현재 on/off 상태.
 * @param onCheckedChange 스위치 상태가 변경되었을 때 호출될 콜백.
 */
@Composable
private fun SwitchSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) } // Row 전체를 클릭해도 스위치가 토글되도록 함
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

/**
 * 설정 항목들을 시각적으로 구분하는 Divider(구분선)입니다.
 *
 * @param modifier 추가적으로 적용할 Modifier.
 */
@Composable
private fun ListDivider(modifier: Modifier = Modifier) {
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        thickness = 1.dp,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    )
}

/**
 * Android Studio에서 SettingsScreen의 UI를 미리보기 위한 Composable 입니다.
 */
@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun SettingsScreenPreview() {
    Day_togetherTheme {
        SettingsScreen(navController = rememberNavController())
    }
}