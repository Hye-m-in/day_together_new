package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.day_together.R
import com.example.day_together.navigation.AppDestinations
import com.example.day_together.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

// 로그인 오류 코드 매핑용 import
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes

// 네이버 SDK import
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.OAuthLoginCallback

// 서버 호출용(Volley) - 더 이상 필요하지 않으므로 주석 처리함
// import com.android.volley.Request
// import com.android.volley.toolbox.JsonObjectRequest
// import com.android.volley.toolbox.Volley
// import org.json.JSONObject

// Firebase 커스텀 토큰 로그인 - 더 이상 필요하지 않으므로 주석 처리
// import com.google.firebase.auth.FirebaseAuth

/**
 * 네이버 SDK 오류 메시지를 사람이 읽기 쉽게 포맷팅
 */
private fun formatNaverError(
    context: Context,
    httpStatus: Int?,
    sdkCode: String?,   // NaverIdLoginSDK.getLastErrorCode()?.code
    sdkDesc: String?    // NaverIdLoginSDK.getLastErrorDescription()
): String {
    val reason = when (sdkCode) {
        "CLIENT_ERROR" -> "앱 설정 문제 가능성이 큽니다.\n- AndroidManifest의 <data android:scheme=\"\${naverClientId}\"/> 주입 확인\n- build.gradle의 manifestPlaceholders[\"naverClientId\"] 설정 확인\n- NidOAuthBridgeActivity 등록/ exported=true 확인"
        "INVALID_REQUEST" -> "요청 파라미터가 유효하지 않습니다. (누락/형식 오류)"
        "UNAUTHORIZED" -> "클라이언트ID/시크릿 불일치 또는 네이버 개발자센터 설정 문제"
        "NETWORK_ERROR" -> "네트워크 오류입니다. 연결 상태를 확인해 주세요."
        "SERVER_ERROR" -> "네이버 서버 오류입니다. 잠시 후 다시 시도해 주세요."
        "USER_CANCEL" -> "사용자가 로그인 과정을 취소했습니다."
        else -> null
    }
    val base = buildString {
        if (reason != null) appendLine(reason)
        if (!sdkCode.isNullOrBlank() || !sdkDesc.isNullOrBlank()) {
            appendLine("상세: [$sdkCode] ${sdkDesc ?: ""}".trim())
        }
        if (httpStatus != null && httpStatus != 0) {
            appendLine("HTTP 상태: $httpStatus")
        }
    }.trim()
    return if (base.isBlank()) "네이버 로그인 실패(원인 미상)" else base
}

/**
 * 로그인 화면의 UI를 그리는 컴포저블 함수
 * 모든 화면 이동은 NavController로 통제
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

    // 비밀번호 표시 여부 상태
    var passwordVisible by remember { mutableStateOf(false) }


    // 구글 로그인 준비 코드
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // ID 토큰 null 방어 및 메시지
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                Toast.makeText(context, "ID 토큰을 가져오지 못했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            // 서버 경유 로그인으로 통일
            // authViewModel.signInWithGoogle(idToken)  (Firebase 직접 로그인)
            // 서버로 idToken을 보내 커스텀 토큰을 받아 Firebase에 로그인
            authViewModel.signInWithGoogleViaServer(idToken)
        } catch (e: ApiException) {
            // 오류 코드별 사용자 메시지 매핑
            val msg = when (e.statusCode) {
                // code=10 → DEVELOPER_ERROR: 설정 불일치 (SHA-1/패키지명/웹 클라이언트 ID)
                CommonStatusCodes.DEVELOPER_ERROR ->
                    "(code=10)앱 설정 오류로 구글 로그인에 실패했습니다.\n관리자에게 문의바랍니다."
                // code=8 → INTERNAL_ERROR: 일시적 네트워크/서비스 불안정
                CommonStatusCodes.INTERNAL_ERROR ->
                    "(code=8)네트워크/서비스가 불안정 합니다. 잠시 후 다시 시도해주세요."
                // 네트워크 오류
                CommonStatusCodes.NETWORK_ERROR ->
                    "네트워크 연결을 확인한 뒤 다시 시도해주세요. (network error)"
                // 사용자가 로그인 창을 닫거나 취소
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                    "로그인이 취소되었습니다."
                // 그 외
                else -> "구글 로그인 실패: ${e.statusCode}"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    // 구글 로그인 준비 코드 끝

    // 로그인 성공 시 메인으로
    LaunchedEffect(key1 = uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
            navController.navigate(AppDestinations.MAIN_ROUTE) {
                popUpTo(AppDestinations.LOGIN_ROUTE) { inclusive = true }
            }
            authViewModel.clearLoginError()
        }
    }

    // 로그인 실패 메시지 표시
    LaunchedEffect(key1 = uiState.loginError) {
        uiState.loginError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            authViewModel.clearLoginError()
        }
    }

    // 화면 UI
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

            // 중앙 컨텐츠
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 이메일
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
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
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

                // 비밀번호
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

                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else
                                Icons.Filled.VisibilityOff

                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "비밀번호 보기/숨기기")
                            }
                        },

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

                // 로그인 버튼
                Button(
                    onClick = authViewModel::login,
                    enabled = uiState.loginEmail.isNotBlank() &&
                            uiState.loginPassword.isNotBlank() &&
                            !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        if (uiState.isLoading) "로그인 중..." else "로그인",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // SNS 로그인
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
                    // 네이버 로그인 (오류 메시지 + 서버 연동)
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_naver, text = "네이버") {
                        NaverIdLoginSDK.authenticate(context, object : OAuthLoginCallback {
                            override fun onSuccess() {
                                val accessToken = NaverIdLoginSDK.getAccessToken()
                                if (accessToken.isNullOrBlank()) {
                                    Toast.makeText(context, "네이버 토큰을 가져오지 못했습니다.", Toast.LENGTH_LONG).show()
                                    return
                                }

                                // ViewModel에 작업 위임
                                authViewModel.onNaverLoginSuccess(accessToken)

                            }

                            override fun onFailure(httpStatus: Int, message: String) {
                                val code = try { NaverIdLoginSDK.getLastErrorCode()?.code } catch (_: Exception) { null }
                                val desc = try { NaverIdLoginSDK.getLastErrorDescription() } catch (_: Exception) { null }
                                val userMsg = formatNaverError(context, httpStatus, code, desc)
                                Toast.makeText(context, userMsg, Toast.LENGTH_LONG).show()
                            }

                            override fun onError(errorCode: Int, message: String) {
                                onFailure(errorCode, message) // SDK 권장 패턴
                            }
                        })
                    }

                    // 구글 로그인 (서버 경유)
                    SocialLoginIconButton(iconRes = R.drawable.ic_logo_google, text = "구글") {
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
            }

            // 하단 메뉴
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
 * SNS 로그인 아이콘 + 텍스트 공통 버튼
 */
@Composable
fun SocialLoginIconButton(
    @DrawableRes iconRes: Int,
    text: String,
    onClick: () -> Unit
) {
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