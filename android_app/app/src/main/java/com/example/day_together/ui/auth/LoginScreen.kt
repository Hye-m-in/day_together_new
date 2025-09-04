package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.navigation.NavController
import com.example.day_together.R
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.*

/**
 * 로그인 화면의 UI를 그리는 컴포저블 함수입
 * 모든 화면 이동은 NavController로
 *
 * @param navController 앱의 화면 전환을 담당하는 NavController
 * @param fromOnboarding 온보딩 화면에서 넘어왔는지 여부(UI 간격 조절용)
 * @param authViewModel 인증 관련 로직을 처리하는 ViewModel
 */
@Composable
fun LoginScreen(
    navController: NavController,
    fromOnboarding: Boolean = false,
    authViewModel: AuthViewModel = viewModel()
) {
    // 상태 및 기본 설정
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // 부가 효과 처리 (Side Effects)
    // 로그인 성공 시 메인 화면으로 이동하는 로직
    LaunchedEffect(key1 = uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
            navController.navigate(AppDestinations.MAIN_ROUTE) {
                popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
            }
            authViewModel.clearLoginError()
        }
    }

    // 로그인 실패 시 에러 메시지 표시 로직
    LaunchedEffect(key1 = uiState.loginError) {
        uiState.loginError?.let {
            // LENGTH_SHORT를 LENGTH_LONG으로 변경하여 표시 시간을 늘림
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
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
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.loginError != null) {
                            Text(
                                text = uiState.loginError!!,
                                color = ErrorRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.loginEmail,
                        onValueChange = authViewModel::onLoginEmailChange,
                        placeholder = { Text("이메일 주소를 입력해주세요") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        isError = uiState.loginError != null,
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
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
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
                    onClick = authViewModel::login,
                    enabled = uiState.loginEmail.isNotBlank() && uiState.loginPassword.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
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
                Text(
                    text = "회원가입",
                    modifier = Modifier.clickable { navController.navigate(AppDestinations.SIGNUP_ROUTE) },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                Text(
                    text = "아이디/비밀번호 찾기",
                    modifier = Modifier.clickable { navController.navigate(AppDestinations.FIND_ACCOUNT_ROUTE) },
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
 * SNS 로그인 아이콘과 텍스트를 함께 보여주는 컴포저블
 *
 * @param iconRes 보여줄 아이콘 이미지의 리소스 ID
 * @param text 아이콘 아래에 표시할 텍스트
 * @param onClick 버튼 클릭 시 실행될 동작
 */
@Composable
fun SocialLoginIconButton(
    @DrawableRes iconRes: Int,
    text: String, // text 파라미터 받도록 추가
    onClick: () -> Unit
) {
    // Column을 사용하여 아이콘과 텍스트를 세로로 배치
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "$text 로그인",
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary.copy(alpha = 0.8f)
        )
    }
}

/**
 * Context에서 현재 실행 중인 Activity를 찾는 확장 함수
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}


/**
 * LoginActivity.kt 파일 삭제 : intent -> navcontroller 사용 방식으로 변경함
 * loginscreen.kt 파일 내 반영 완료
 */
