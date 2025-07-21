package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.day_together.ui.theme.*

/**
 * 아이디/비밀번호 찾기 화면의 전체 UI를 그리는 컴포저블 함수
 *
 *  컴포저블 함수(= 가구 설계도, 화면에 어떻게 보일 것인지 설명)
 *  1. 목적 : ui 설명 및 화면 그리기
 *  2. 어노테이션(=꼬리표 또는 주석) : @Composable 필수
 *  3. 값 반환 없음
 *  4. 다른 @Composable 함수 안에서만 호출 가능
 *  5. 핵심 역할 : 어떻게 보일 것인가?

 * @param authViewModel 인증 관련 데이터와 로직을 처리하는 ViewModel
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindAccountScreen(
    authViewModel: AuthViewModel = viewModel()
) {


    /** 상태 및 기본 설정
     *
     * 인텐트(Intent) : 앱 구성요소끼리 주고받는 메시지 또는 요청서
     *
     * 인텐트 예시) LoginActiviy에서 회원가입 버튼 눌렀을 때
     * 1. 편지지(Intent) 준비, 수신인은 SignUpActivity
     * val intent = Intent(context, SignUpActivity::class.java)
     * 2. 편지 우체통(시스템)에 넣기
     * context.startActivity(intent)
     * 3. SignUpActivity라는 화면 띄워줌
     *
     *
     * 키보드 포커스 : 현재 키보드 입력 받을 준비가 완료된 ui 요소
     * -> 여러 개 텍스트 입력 칸 중 어디에 커서가 깜빡이고 있는가?
     */

    // ViewModel의 UI 상태를 구독 해서, 상태가 바뀔 때마다 화면이 자동으로 새로 그려지게 함
    val uiState by authViewModel.uiState.collectAsState()
    // Toast 메시지나 Intent 사용을 위해 현재 Context를 가져옴
    val context = LocalContext.current
    // 키보드 포커스(커서)를 관리하기 위한 도구
    val focusManager = LocalFocusManager.current
    // 화면이 길어질 경우 스크롤을 가능하게 함
    val scrollState = rememberScrollState()
    // 뒤로가기 버튼을 위해 현재 화면의 액티비티를 찾음
    val activity = context.findActivity()




    // 부가 효과 처리

    // `findAccountResult` 상태가 바뀔 때마다 실행되는 코드 블록
    LaunchedEffect(key1 = uiState.findAccountResult) {
        // 결과가 null이 아닐 때만 실행 (즉, 아이디/비밀번호 찾기 요청이 끝났을 때)
        uiState.findAccountResult?.let { result ->
            // 성공/실패에 따라 다른 메시지를 정함
            val message = when (result) {
                is AuthResult.Success -> "요청 성공! 이메일을 확인해주세요."
                is AuthResult.Failure -> result.message // 실패 시엔 ViewModel이 보내준 에러 메시지 사용
            }
            // 사용자에게 토스트 메시지로 결과를 보여줌
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // 메시지를 한 번 보여준 후에는, ViewModel의 상태를 깨끗하게 초기화해서 중복 표시를 방지
            authViewModel.clearFindAccountResult()
        }
    }

    // UI 상태에 따른 변수 계산

    // 비밀번호 찾기 버튼 활성화 조건: 이름과 이메일이 비어있지 않고, 로딩 중이 아닐 때
    val isFindPwButtonEnabled = uiState.findPwName.isNotBlank() && uiState.findPwEmail.isNotBlank() && !uiState.isLoading
    // 아이디 찾기 버튼 활성화 조건: 이름과 이메일이 비어있지 않고, 로딩 중이 아닐 때
    val isFindIdButtonEnabled = uiState.findIdName.isNotBlank() && uiState.findIdEmail.isNotBlank() && !uiState.isLoading



    // 화면 UI 구성

    Day_togetherTheme {
        // Scaffold: 상단바, 본문 등 기본적인 화면 구조를 잡아주는 틀
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* 제목은 비워둠 */ },
                    navigationIcon = {
                        // 뒤로가기 아이콘 버튼
                        IconButton(onClick = { activity?.finish() }) { // 클릭하면 현재 액티비티 종료
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
                )
            }
        ) { innerPadding -> // 상단바 영역 제외한 나머지 공간
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScreenBackground)
                    .padding(innerPadding) // 상단바 높이만큼 패딩을 줘서 내용이 겹치지 않게 함
                    .padding(horizontal = 32.dp, vertical = 24.dp)
                    .verticalScroll(scrollState), // 스크롤 가능하게 설정
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 비밀번호 찾기 섹션
                Text(
                    text = "비밀번호 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                // 이름 입력 필드
                FindAccountTextField(
                    label = "이름",
                    value = uiState.findPwName,
                    onValueChange = authViewModel::onFindPwNameChange,
                    imeAction = ImeAction.Next, // 키보드 액션 버튼: '다음'
                    focusManager = focusManager
                )
                // 이메일 입력 필드
                FindAccountTextField(
                    label = "아이디(이메일)",
                    value = uiState.findPwEmail,
                    onValueChange = authViewModel::onFindPwEmailChange,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done, // 키보드 액션 버튼: '완료'
                    focusManager = focusManager,
                    onDone = { // '완료' 버튼 클릭 시
                        focusManager.clearFocus() // 키보드 숨기기
                        if(isFindPwButtonEnabled) { authViewModel.resetPassword() } // 버튼 활성화 상태인 경우, 비밀번호 재설정 요청
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                // 비밀번호 재설정 버튼
                Button(
                    onClick = {
                        focusManager.clearFocus() // 키보드 숨기기
                        authViewModel.resetPassword() // ViewModel에 비밀번호 재설정 로직 실행 요청
                    },
                    enabled = isFindPwButtonEnabled, // 버튼 활성화/비활성화 상태
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    Text("비밀번호 재설정", style = MaterialTheme.typography.labelMedium)
                }

                // 구분선
                Divider(modifier = Modifier.padding(vertical = 32.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // 아이디 찾기 섹션
                Text(
                    text = "아이디 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                // 이름 입력 필드
                FindAccountTextField(
                    label = "이름",
                    value = uiState.findIdName,
                    onValueChange = authViewModel::onFindIdNameChange,
                    imeAction = ImeAction.Next,
                    focusManager = focusManager
                )
                // 이메일 입력 필드
                FindAccountTextField(
                    label = "이메일",
                    value = uiState.findIdEmail,
                    onValueChange = authViewModel::onFindIdEmailChange,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    focusManager = focusManager,
                    onDone = {
                        focusManager.clearFocus()
                        if(isFindIdButtonEnabled) { authViewModel.findId() }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 안내 문구
                Text(
                    text = "(입력하신 메일 주소와 일치하는 아이디를 찾아 해당 메일 주소로 전송합니다)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = TextPrimary.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                // 아이디 찾기 버튼
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.findId() // ViewModel에 아이디 찾기 로직 실행 요청
                    },
                    enabled = isFindIdButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    Text("아이디 찾기", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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