package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.day_together.MainActivity
import com.example.day_together.R
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    fromOnboarding: Boolean = false,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(key1 = uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)

            // 안전한 방식으로 현재 액티비티 종료
            context.findActivity()?.finish()
        }
    }

    LaunchedEffect(key1 = uiState.loginError) {
        uiState.loginError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            authViewModel.clearLoginError()
        }
    }

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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {

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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            focusManager.moveFocus(FocusDirection.Down)
                        }),
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
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
                Button(
                    onClick = authViewModel::login,
                    enabled = uiState.loginEmail.isNotBlank() && uiState.loginPassword.isNotBlank() && !uiState.isLoading,
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
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_naver, text = "네이버") { }
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_kakao, text = "카카오") { }
                }
            }
            Column(
                modifier = Modifier.padding(bottom = if (fromOnboarding) 60.dp else 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClickableText(
                    text = AnnotatedString("회원가입"),
                    onClick = {
                        context.startActivity(Intent(context, SignUpActivity::class.java))
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                ClickableText(
                    text = AnnotatedString("아이디/비밀번호 찾기"),
                    onClick = {
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
            modifier = Modifier.fillMaxSize(0.75f)
        )
    }
}

// Context에서 Activity를 안전하게 찾아오는 헬퍼 함수
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}