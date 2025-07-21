package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day_together.MainActivity
import com.example.day_together.R
import com.example.day_together.ui.theme.*

/**
 * 로그인 화면의 전체 UI를 그리는 컴포저블 함수 : 화면에 보일 내용을 설계하는 설계도 역할
 *
 * @param fromOnboarding 온보딩 화면에서 넘어왔는지(UI 간격 조절에 사용)
 * @param authViewModel 인증 관련 데이터와 로직을 처리하는 ViewModel
 */
@Composable
fun LoginScreen(
    fromOnboarding: Boolean = false,
    authViewModel: AuthViewModel = viewModel()
) {
    // 상태 및 기본 설정
    // ViewModel의 UI 상태(uiState)를 실시간으로 관찰 -> uiState가 바뀌면 화면이 자동으로 새로 그려짐
    val uiState by authViewModel.uiState.collectAsState()
    // Toast 메시지나 화면 이동(Intent)을 위해 현재 Context 가져옴
    val context = LocalContext.current
    // 키보드 포커스(커서 관리)용 FocusManager 가져옴
    val focusManager = LocalFocusManager.current
    // 화면 스크롤을 위한 ScrollState 생성
    val scrollState = rememberScrollState()


    // 부가 효과 처리
    // LaunchedEffect: 특정 상태가 변경될 때만 코드 실행하는 컴포저블

    // isLoginSuccess 상태가 변경될 때마다 실행
    LaunchedEffect(key1 = uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
            // 메인 화면으로 이동하는 Intent(요청서) 생성
            val intent = Intent(context, MainActivity::class.java).apply {
                // 이전 화면 기록을 모두 지워서 뒤로가기 시 로그인 화면으로 돌아오지 않도록 설정
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            // Intent를 시스템에 전달하여 화면 이동 실행
            context.startActivity(intent)
            // 현재 로그인 액티비티 종료
            context.findActivity()?.finish()
        }
    }

    // loginError 상태가 변경될 때마다 실행
    LaunchedEffect(key1 = uiState.loginError) {
        // 에러 메시지가 null이 아닐 경우
        uiState.loginError?.let {
            // 토스트 메시지로 에러 내용 표시
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            // 에러를 표시한 후에는 ViewModel의 상태를 초기화해서 중복 표시 방지
            authViewModel.clearLoginError()
        }
    }

    // 화면 UI 구성
    Day_togetherTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            // SpaceBetween: 자식 요소들 위, 중간, 아래에 공간 두고 배치
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단 여백
            Spacer(modifier = Modifier.height(if (fromOnboarding) 80.dp else 120.dp))

            // 중앙 컨텐츠 (입력 필드, 버튼 등)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 이메일 입력 섹션
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ID(Email)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f) // 남은 공간 모두 차지
                        )
                        // 로그인 에러가 있을 경우에만 에러 메시지 표시
                        if (uiState.loginError != null) {
                            Text(
                                text = uiState.loginError!!, // !!: null이 아님을 확신하고 사용
                                color = ErrorRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.loginEmail, // 값은 항상 ViewModel의 상태를 따름
                        onValueChange = authViewModel::onLoginEmailChange, // 값이 바뀔 때마다 ViewModel에 알림
                        placeholder = { Text("이메일 주소를 입력해주세요") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email, // 이메일 형식 키보드
                            imeAction = ImeAction.Next // 키보드 '다음' 액션 버튼
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            focusManager.moveFocus(FocusDirection.Down) // '다음' 누르면 비밀번호 칸으로 이동
                        }),
                        isError = uiState.loginError != null, // 에러 상태일 때 테두리 색 변경
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (uiState.loginError != null) ErrorRed else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (uiState.loginError != null) ErrorRed else MaterialTheme.colorScheme.outline,
                            errorBorderColor = ErrorRed,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 비밀번호 입력 섹션
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.loginPassword,
                        onValueChange = authViewModel::onLoginPasswordChange,
                        placeholder = { Text("비밀번호를 입력해주세요") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), // 입력 내용 가림
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password, // 비밀번호 형식 키보드
                            imeAction = ImeAction.Done // 키보드 액션 버튼: '완료'
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus() // '완료' 누르면 키보드 숨기기
                            // 이메일과 비밀번호가 모두 입력됐으면 바로 로그인 시도
                            if (uiState.loginEmail.isNotBlank() && uiState.loginPassword.isNotBlank()) {
                                authViewModel.login()
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                // 로그인 버튼 
                Button(
                    onClick = authViewModel::login, // 클릭 시 ViewModel의 login 함수 실행
                    enabled = uiState.loginEmail.isNotBlank() && uiState.loginPassword.isNotBlank() && !uiState.isLoading, // 활성화 조건
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = ButtonDisabledText
                    )
                ) {
                    Text(if (uiState.isLoading) "로그인 중..." else "로그인", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(40.dp))
                // SNS 로그인 섹션
                Text(
                    "SNS 계정으로 로그인",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_naver, text = "네이버") { /* TODO: 네이버 로그인 구현 */ }
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_google, text = "구글") { /* TODO: 구글 로그인 구현 */ }
                }
            }
            // 하단 메뉴 (회원가입, 계정 찾기)
            Column(
                modifier = Modifier.padding(bottom = if (fromOnboarding) 60.dp else 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 일반 Text에 clickable 수정자를 붙여서 클릭 가능하게 만듦
                Text(
                    text = "회원가입",
                    modifier = Modifier.clickable {
                        // 클릭 시 회원가입 화면(SignUpActivity)으로 이동
                        context.startActivity(Intent(context, SignUpActivity::class.java))
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        textDecoration = TextDecoration.Underline // 밑줄 효과
                    )
                )
                Text(
                    text = "아이디/비밀번호 찾기",
                    modifier = Modifier.clickable {
                        // 클릭 시 계정 찾기 화면(FindAccountActivity)으로 이동
                        context.startActivity(Intent(context, FindAccountActivity::class.java))
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
    }
}

/**
 * @param iconRes 보여줄 아이콘 이미지의 리소스 ID
 * @param onClick 버튼 클릭 시 실행될 동작
 */
@Composable
fun SocialLoginIconButton(
    @DrawableRes iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(68.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "$text 로그인",
            modifier = Modifier.fillMaxSize(0.75f) // 아이콘 크기 버튼보다 약간 작게 조절
        )
    }
}

/**
 * Context에서 우리가 찾고자 하는 Activity를 찾아오는 함수
 */

// this는 현재 context
private fun Context.findActivity(): Activity? = when (this) {
    // 실제 우리가 찾고자 하는 activity인 경우, 즉시 반환
    is Activity -> this
    // 찾고자하는 activity가 아니라면( = 안쪽 context라면), 안쪽 context 반환
    is ContextWrapper -> baseContext.findActivity()
    // 못찾을 경우, null 반환
    else -> null
}