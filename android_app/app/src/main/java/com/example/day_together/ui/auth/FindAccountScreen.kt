package com.example.day_together.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.day_together.data.repository.AuthResult
import com.example.day_together.ui.theme.*

/**
 * 아이디/비밀번호 찾기 화면의 UI를 그리는 컴포저블 함수
 * 모든 화면 이동은 NavController로
 *
 * @param navController 앱의 화면 전환을 담당하는 NavController
 * @param authViewModel 인증 관련 로직을 처리하는 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindAccountScreen(
    // AppNavigation에서 NavController를 직접 받도록 파라미터 추가
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // 상태 및 기본 설정
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()


    // 부가 효과 처리: 계정 찾기 결과에 따라 Toast 메시지 출력
    LaunchedEffect(key1 = uiState.findAccountResult) {
        uiState.findAccountResult?.let { result ->
            val message = when (result) {
                is AuthResult.Success -> "요청 성공! 이메일을 확인해주세요."
                is AuthResult.Failure -> result.message
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            authViewModel.clearFindAccountResult()
        }
    }

    // UI 상태에 따른 버튼 활성화 조건 계산
    val isFindPwButtonEnabled = uiState.findPwName.isNotBlank() && uiState.findPwEmail.isNotBlank() && !uiState.isLoading


    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* 제목 없음 */ },
                    navigationIcon = {
                        // 뒤로가기 버튼 클릭 시, NavController를 사용하여 이전 화면으로 돌아감
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBackground)
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 비밀번호 찾기 섹션
                Text(
                    text = "비밀번호 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                FindAccountTextField(
                    label = "이름", value = uiState.findPwName, onValueChange = authViewModel::onFindPwNameChange,
                    imeAction = ImeAction.Next, focusManager = focusManager
                )
                FindAccountTextField(
                    label = "아이디(이메일)", value = uiState.findPwEmail, onValueChange = authViewModel::onFindPwEmailChange,
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Done, focusManager = focusManager,
                    onDone = {
                        focusManager.clearFocus()
                        if (isFindPwButtonEnabled) { authViewModel.resetPassword() }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.resetPassword()
                    },
                    enabled = isFindPwButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) { Text("비밀번호 재설정", style = MaterialTheme.typography.labelMedium) }

                // Divider 및 아이디 찾기 섹션 전체 삭제


                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// 공용 텍스트 필드
@Composable
private fun FindAccountTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction,
    focusManager: FocusManager,
    onDone: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { onDone?.invoke() ?: focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}