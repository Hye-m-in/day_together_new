package com.example.day_together.ui.auth

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.R
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.Day_togetherTheme
import com.example.day_together.ui.theme.ButtonActiveBackground
import com.example.day_together.ui.theme.ButtonActiveText
import com.example.day_together.ui.theme.ButtonDisabledBackground
import com.example.day_together.ui.theme.ButtonDisabledText
import com.example.day_together.ui.theme.ErrorRed
import com.example.day_together.ui.theme.TextPrimary


data class LoginScreenState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val fromOnboarding: Boolean = false,
    val simulateFilled: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    fromOnboarding: Boolean = false,
    initialEmail: String = "",
    initialPassword: String = "",
    initialEmailError: String? = null
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf(initialPassword) }
    var emailError by remember { mutableStateOf(initialEmailError) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val isLoginButtonEnabled = email.isNotBlank() && password.isNotBlank()

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
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (emailError != null) {
                            Text(
                                text = emailError!!,
                                color = ErrorRed,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            emailError = null
                        },
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
                        isError = emailError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (emailError != null) ErrorRed else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (emailError != null) ErrorRed else MaterialTheme.colorScheme.outline,
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
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
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
                            if (isLoginButtonEnabled) {
                                // TODO: 로그인 시도
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
                    onClick = { /* ... */ },
                    enabled = isLoginButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = ButtonDisabledText
                    )
                ) {
                    Text("로그인", style = MaterialTheme.typography.labelLarge)
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
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_naver, text = "네이버") { /* ... */ }
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_kakao, text = "카카오") { /* ... */ }
                }
            }
            Column(
                modifier = Modifier.padding(bottom = if (fromOnboarding) 60.dp else 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClickableText(
                    text = AnnotatedString("회원가입"),
                    onClick = { navController.navigate(AppDestinations.SIGNUP_ROUTE) },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary,
                        textDecoration = TextDecoration.Underline
                    )
                )
                ClickableText(
                    text = AnnotatedString("아이디/비밀번호 찾기"),
                    onClick = { navController.navigate(AppDestinations.FIND_ACCOUNT_ROUTE) },
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

// --- Preview 함수 ---


class LoginScreenStateProvider : PreviewParameterProvider<LoginScreenState> {
    override val values = sequenceOf(
        LoginScreenState(), // 기본 (비활성 버튼)
        LoginScreenState(email = "test", emailError = "올바르지 않은 이메일 형식입니다."),
        LoginScreenState(email = "test@example.com", password = "password123", simulateFilled = true)
    )
}

@Preview(showBackground = true, name = "Login States (From Onboarding)", widthDp = 390, heightDp = 844)
@Composable
fun LoginScreenAllStatesFromOnboardingPreview(
    @PreviewParameter(LoginScreenStateProvider::class) state: LoginScreenState
) {
    Day_togetherTheme {
        LoginScreen(
            navController = rememberNavController(),
            fromOnboarding = true,
            initialEmail = state.email,
            initialPassword = if (state.simulateFilled) state.password else "",
            initialEmailError = state.emailError
        )
    }
}

@Preview(showBackground = true, name = "Login States (Standalone)", widthDp = 390, heightDp = 844)
@Composable
fun LoginScreenAllStatesStandalonePreview(
    @PreviewParameter(LoginScreenStateProvider::class) state: LoginScreenState
) {
    Day_togetherTheme {
        LoginScreen(
            navController = rememberNavController(),
            fromOnboarding = false,
            initialEmail = state.email,
            initialPassword = if (state.simulateFilled) state.password else "",
            initialEmailError = state.emailError
        )
    }
}