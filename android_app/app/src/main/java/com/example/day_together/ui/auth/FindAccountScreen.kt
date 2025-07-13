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
import androidx.navigation.NavController
import com.example.day_together.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindAccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // 현재 액티비티를 가져옴
    val activity = context.findActivity()

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

    val isFindPwButtonEnabled = uiState.findPwName.isNotBlank() && uiState.findPwEmail.isNotBlank() && !uiState.isLoading
    val isFindIdButtonEnabled = uiState.findIdName.isNotBlank() && uiState.findIdEmail.isNotBlank() && !uiState.isLoading

    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {  },
                    navigationIcon = {
                        // 뒤로가기 버튼 클릭 시 현재 액티비티 종료
                        IconButton(onClick = { activity?.finish() }) {
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
                // 비밀번호 찾기
                Text(
                    text = "비밀번호 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                FindAccountTextField(
                    label = "이름",
                    value = uiState.findPwName,
                    onValueChange = authViewModel::onFindPwNameChange,
                    imeAction = ImeAction.Next,
                    focusManager = focusManager
                )
                FindAccountTextField(
                    label = "아이디(이메일)",
                    value = uiState.findPwEmail,
                    onValueChange = authViewModel::onFindPwEmailChange,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                    focusManager = focusManager,
                    onDone = {
                        focusManager.clearFocus()
                        if(isFindPwButtonEnabled) { authViewModel.resetPassword() }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.resetPassword()
                    },
                    enabled = isFindPwButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
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

                // 아이디 찾기
                Text(
                    text = "아이디 찾기",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                FindAccountTextField(
                    label = "이름",
                    value = uiState.findIdName,
                    onValueChange = authViewModel::onFindIdNameChange,
                    imeAction = ImeAction.Next,
                    focusManager = focusManager
                )
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
                Text(
                    text = "(입력하신 메일 주소와 일치하는 아이디를 찾아 메일 주소로 전송합니다)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, color = TextPrimary.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        authViewModel.findId()
                    },
                    enabled = isFindIdButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
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

// Context에서 Activity를 안전하게 찾아오는 헬퍼 함수
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}