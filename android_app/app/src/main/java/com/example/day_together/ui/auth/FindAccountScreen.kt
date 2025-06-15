package com.example.day_together.ui.auth

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.day_together.ui.theme.*


data class FindAccountScreenState(

    val findPwName: String = "",
    val findPwEmail: String = "",

    val findIdName: String = "",
    val findIdEmail: String = "",
    val simulatePwFieldsFilled: Boolean = false,
    val simulateIdFieldsFilled: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindAccountScreen(
    navController: NavController,
    initialState: FindAccountScreenState = FindAccountScreenState()
) {
    // 비밀번호 찾기 상태
    var findPwName by remember { mutableStateOf(initialState.findPwName) }
    var findPwEmail by remember { mutableStateOf(initialState.findPwEmail) }

    // 아이디 찾기 상태
    var findIdName by remember { mutableStateOf(initialState.findIdName) }
    var findIdEmail by remember { mutableStateOf(initialState.findIdEmail) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // 버튼 활성화 조건
    val isFindPwButtonEnabled = findPwName.isNotBlank() && findPwEmail.isNotBlank() && findPwEmail.contains("@")
    val isFindIdButtonEnabled = findIdName.isNotBlank() && findIdEmail.isNotBlank() && findIdEmail.contains("@")

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {  },
                    navigationIcon = {
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
                // --- 비밀번호 찾기 섹션 ---
                Text(
                    text = "비밀번호 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                FindAccountTextField(
                    label = "이름",
                    value = findPwName,
                    onValueChange = { findPwName = it },
                    imeAction = ImeAction.Next,
                    focusManager = focusManager
                )
                FindAccountTextField(
                    label = "아이디(이메일)",
                    value = findPwEmail,
                    onValueChange = { findPwEmail = it },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    focusManager = focusManager,
                    onDone = {
                        focusManager.clearFocus()
                        if(isFindPwButtonEnabled) { /*TODO: 비밀번호 찾기 로직*/ }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        // TODO: 비밀번호 찾기 로직 (메일 전송 등)
                    },
                    enabled = isFindPwButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(48.dp), // 버튼 높이 조절
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

                Divider(modifier = Modifier.padding(vertical = 32.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                // --- 아이디 찾기 섹션 ---
                Text(
                    text = "아이디 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                FindAccountTextField(
                    label = "이름",
                    value = findIdName,
                    onValueChange = { findIdName = it },
                    imeAction = ImeAction.Next,
                    focusManager = focusManager
                )
                FindAccountTextField(
                    label = "이메일",
                    value = findIdEmail,
                    onValueChange = { findIdEmail = it },
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    focusManager = focusManager,
                    onDone = {
                        focusManager.clearFocus()
                        if(isFindIdButtonEnabled) { /*TODO: 아이디 찾기 로직*/ }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text( // 안내 문구
                    text = "(입력하신 메일 주소와 일치하는 아이디를 찾아 메일 주소로 전송합니다)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = TextPrimary.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        // TODO: 아이디 찾기 로직 (메일 전송 등)
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

@Composable
private fun FindAccountTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction,
    focusManager: FocusManager,
    onDone: (() -> Unit)? = null,
    placeholder: String? = null
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
            placeholder = { if (placeholder != null) Text(placeholder, color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
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
                cursorColor = MaterialTheme.colorScheme.primary,
            )
        )
    }
}

// --- Preview 함수 ---
class FindAccountScreenStateProvider : PreviewParameterProvider<FindAccountScreenState> {
    override val values = sequenceOf(
        FindAccountScreenState(), // 기본 (모든 버튼 비활성)
        FindAccountScreenState(findPwName = "홍길동", findPwEmail = "test@example.com", simulatePwFieldsFilled = true), // 비밀번호 찾기 활성
        FindAccountScreenState(findIdName = "김철수", findIdEmail = "user@email.net", simulateIdFieldsFilled = true), // 아이디찾기 활성
        FindAccountScreenState( // 모두 채워진 상태
            findPwName = "홍길동", findPwEmail = "test@example.com", simulatePwFieldsFilled = true,
            findIdName = "김철수", findIdEmail = "user@email.net", simulateIdFieldsFilled = true
        )
    )
}

@Preview(showBackground = true, name = "Find Account Screen States", widthDp = 390, heightDp = 844)
@Composable
fun FindAccountScreenAllStatesPreview(
    @PreviewParameter(FindAccountScreenStateProvider::class) state: FindAccountScreenState
) {
    Day_togetherTheme {
        FindAccountScreen(
            navController = rememberNavController(),
            initialState = state
        )
    }
}