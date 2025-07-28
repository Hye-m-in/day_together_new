package com.example.day_together.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.day_together.MainActivity
import com.example.day_together.data.repository.AuthResult // ViewModel이 사용하는 AuthResult 클래스 import
import com.example.day_together.ui.theme.*

/**
 * 회원가입 화면의 전체 UI를 구성하고 비즈니스 로직을 연결하는 메인 컴포저블 함수입니다.
 * @param navController 화면 이동을 제어하는 NavController 객체입니다.
 * @param authViewModel 인증 관련 상태와 로직을 관리하는 ViewModel입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // --- 1. 상태 및 기본 설정 초기화 ---

    // ViewModel의 uiState를 구독, uiState가 변경되면 이 컴포저블은 자동으로 리컴포지션(재구성)
    val uiState by authViewModel.uiState.collectAsState()
    // 현재 컨텍스트(Context)를 가져옴 -> Toast 메시지나 Intent 실행 등에 필요
    val context = LocalContext.current
    // 포커스(키보드 커서)를 관리하는 객체 -> 다음 입력 필드로 이동시키거나 키보드를 숨길 때 사용
    val focusManager = LocalFocusManager.current
    // 화면이 길어질 경우 스크롤을 가능하게 하기 위한 상태 객체입니다.
    val scrollState = rememberScrollState()
    // 현재 화면이 속한 액티비티(Activity) 찾고 & 화면을 종료(finish)할 때 필요
    val activity = context.findActivity()

    // 비밀번호 필드의 "****" 표시 여부를 관리하는 상태 변수
    var passwordVisible by remember { mutableStateOf(false) }
    // 비밀번호 확인 필드의 "****" 표시 여부를 관리하는 상태 변수
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // 갤러리에서 이미지를 선택하고 그 결과를 받아오기 위한 런처
    // 사용자가 이미지를 선택하면 onResult 콜백이 실행
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(), // 안드로이드의 컨텐츠(이미지, 파일 등)를 가져오는 계약
        onResult = { uri -> // 사용자가 이미지를 선택했을 때 호출될 함수
            if (uri != null) { // 선택된 이미지의 Uri가 null이 아닐 경우
                authViewModel.onProfileImageChanged(uri) // ViewModel에 변경된 이미지 Uri를 전달
            }
        }
    )

    // --- 2. 부가 효과(Side Effects) 처리 ---

    // isSignUpAndLoginSuccess 상태가 변경될 때마다 실행되는 효과
    // LaunchedEffect는 컴포지션 생명주기 내에서 코루틴을 안전하게 실행하기 위해 사용
    LaunchedEffect(key1 = uiState.isSignUpAndLoginSuccess) {
        if (uiState.isSignUpAndLoginSuccess) { // 회원가입과 로그인이 모두 성공했다면,
            // 사용자에게 성공 메시지를 보여줌
            Toast.makeText(context, "회원가입 및 로그인 성공!", Toast.LENGTH_SHORT).show()
            // 메인 화면으로 이동하기 위한 Intent를 생성
            val intent = Intent(context, MainActivity::class.java).apply {
                // 이전의 모든 액티비티 스택에서 제거하고, 새로운 태스크에서 메인 액티비티 시작
                // 이렇게 하면 사용자가 메인 화면에서 뒤로가기 버튼을 눌렀을 때 로그인/회원가입 화면으로 돌아가지 않음
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent) // 메인 화면으로 이동
            activity?.finish() // 현재 회원가입 액티비티 종료
        }
    }

    // signUpResult 상태가 변경될 때마다 실행되는 효과(주로 실패 처리)
    LaunchedEffect(key1 = uiState.signUpResult) {
        uiState.signUpResult?.let { result -> // 결과가 null이 아닐 때만 실행
            if (result is AuthResult.Failure) { // 결과가 '실패' 상태일 경우
                // ViewModel로부터 받은 에러 메시지를 Toast로 보여줌
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                // Toast를 한 번 보여준 후에는 ViewModel의 상태를 초기화하여,
                // 화면 회전 등 리컴포지션이 발생해도 Toast가 중복으로 뜨지 않도록 방지
                authViewModel.clearSignUpResult()
            }
        }
    }

    // --- 3. UI 상태에 따른 동적 변수 계산 ---

    // 가족 구성원 선택 여부를 계산(기본 목록 중 하나라도 선택했거나, '기타'를 체크하고 내용을 입력했는지 확인)
    val isFamilyMemberSelected = uiState.familyMemberSelections.values.any { it } ||
            (uiState.otherFamilyMemberChecked && uiState.otherFamilyMemberText.isNotBlank())

    // '회원가입' 버튼의 활성화 여부를 결정하는 조건
    // 모든 필수 정보가 입력되고, 유효성 검사 에러가 없으며, 로딩 중이 아닐 때만 true
    val isSignUpButtonEnabled = uiState.signUpName.isNotBlank() &&
            uiState.signUpBirthDate.length == 8 &&
            uiState.signUpEmail.isNotBlank() &&
            uiState.signUpPassword.isNotBlank() &&
            uiState.signUpConfirmPassword.isNotBlank() &&
            uiState.signUpEmailError == null &&
            uiState.signUpPasswordError == null &&
            uiState.signUpConfirmPasswordError == null &&
            uiState.signUpBirthDateError == null &&
            isFamilyMemberSelected && !uiState.isLoading

    // --- 4. UI 레이아웃 구성 ---

    Day_togetherTheme {
        // Scaffold는 상단바, 하단바, 본문 등 Material Design의 기본적인 화면 구조 제공
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { /* 제목은 비워둠 */ },
                    navigationIcon = {
                        // 뒤로가기 아이콘 버튼
                        IconButton(onClick = { activity?.finish() }) { // 클릭 시 현재 액티비티를 종료합니다.
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
                )
            }
        ) { innerPadding -> // 상단바 영역을 제외한 나머지 본문 영역
            Column(
                modifier = Modifier
                    .fillMaxSize() // 화면 전체를 채움
                    .background(ScreenBackground) // 배경색 설정
                    .padding(innerPadding) // 상단바 높이만큼 패딩을 주어 내용이 겹치지 않게 함
                    .padding(horizontal = 32.dp, vertical = 16.dp) // 좌우, 상하 추가 패딩
                    .verticalScroll(scrollState), // 스크롤 가능하게 설정
                horizontalAlignment = Alignment.CenterHorizontally // 자식 요소들을 가로축 중앙에 정렬
            ) {
                // 프로필 사진을 선택하는 영역
                Box(
                    modifier = Modifier
                        .size(100.dp) // 크기 지정
                        .clip(CircleShape) // 원형으로 자름
                        .border( // 테두리 설정
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { galleryLauncher.launch("image/*") }, // 클릭 시 갤러리 런처 실행
                    contentAlignment = Alignment.Center // 내부 컨텐츠를 중앙에 정렬
                ) {
                    if (uiState.profileImageUri != null) { // 선택된 이미지가 있다면,
                        // Coil 라이브러리의 AsyncImage를 사용해 비동기적으로 이미지를 로드하고 표시
                        AsyncImage(
                            model = uiState.profileImageUri,
                            contentDescription = "선택된 프로필 이미지",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop // 이미지가 Box를 꽉 채우도록
                        )
                    } else { // 선택된 이미지가 없다면,
                        // 기본 안내 텍스트를 표시합니다.
                        Text("사진 추가", fontSize = 12.sp, color = TextPrimary.copy(alpha=0.7f))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp)) // 수직 여백

                // 이름 입력 필드
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    SignUpTextField(label = "이름", value = uiState.signUpName, onValueChange = authViewModel::onSignUpNameChange, imeAction = ImeAction.Next, focusManager = focusManager)
                }

                // 생년월일 입력 섹션
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "생년월일",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 양력/음력 선택 체크박스
                        SolarLunarCheckbox(text = "양력", checked = !uiState.signUpIsLunar, onCheckedChange = { if (it) authViewModel.onSignUpIsLunarChange(false) })
                        Spacer(modifier = Modifier.width(16.dp))
                        SolarLunarCheckbox(text = "음력", checked = uiState.signUpIsLunar, onCheckedChange = { if (it) authViewModel.onSignUpIsLunarChange(true) })
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.signUpBirthDate,
                        onValueChange = authViewModel::onSignUpBirthDateChange,
                        placeholder = { Text("ex)20040506", color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }), // '다음' 버튼 클릭 시 아래 필드로 포커스 이동
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            // 에러 상태에 따라 테두리 색상을 동적으로 변경
                            focusedBorderColor = if(uiState.signUpBirthDateError != null) ErrorRed else MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if(uiState.signUpBirthDateError != null) ErrorRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                        isError = uiState.signUpBirthDateError != null, // 에러 상태 전달
                        supportingText = { // 필드 하단에 표시될 보조 텍스트(주로 에러 메시지용)
                            val error = uiState.signUpBirthDateError
                            if (error != null) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    )
                }

                // 이메일 입력 필드
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SignUpTextField(label = "ID(Email)", value = uiState.signUpEmail, onValueChange = authViewModel::onSignUpEmailChange, placeholder = "이메일@도메인.com", keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, focusManager = focusManager, error = uiState.signUpEmailError)
                }

                // 비밀번호 입력 필드
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    SignUpTextField(
                        label = "Password", value = uiState.signUpPassword, onValueChange = authViewModel::onSignUpPasswordChange,
                        placeholder = "영문,숫자,특수기호 포함 8자 이상",
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), // 'passwordVisible' 상태에 따라 텍스트 또는 '****' 표시
                        imeAction = ImeAction.Next, focusManager = focusManager, error = uiState.signUpPasswordError,
                        trailingIcon = { // 입력 필드 끝에 표시될 아이콘
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) { // 클릭 시 'passwordVisible' 상태를 토글
                                Icon(imageVector = image, contentDescription = "비밀번호 보기 토글")
                            }
                        }
                    )
                }

                // 비밀번호 확인 입력 필드
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    SignUpTextField(
                        label = "비밀번호 확인", value = uiState.signUpConfirmPassword, onValueChange = authViewModel::onSignUpConfirmPasswordChange,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        imeAction = ImeAction.Done, focusManager = focusManager, onDone = { focusManager.clearFocus() }, // '완료' 버튼 클릭 시 키보드 숨김
                        error = uiState.signUpConfirmPasswordError,
                        trailingIcon = {
                            val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(imageVector = image, contentDescription = "비밀번호 확인 보기 토글")
                            }
                        }
                    )
                }

                // 가족 구성원 선택 섹션
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    FamilyMemberSelection(
                        title = "가족 구성원 중 나는?",
                        members = listOf("할아버지", "할머니", "아버지", "어머니", "아들", "딸"),
                        selections = uiState.familyMemberSelections,
                        onSelectionChange = authViewModel::onFamilyMemberSelectionChange,
                        otherChecked = uiState.otherFamilyMemberChecked,
                        onOtherCheckedChange = authViewModel::onOtherFamilyMemberCheckedChange,
                        otherText = uiState.otherFamilyMemberText,
                        onOtherTextChange = authViewModel::onOtherFamilyMemberTextChange,
                        focusManager = focusManager
                    )
                }

                // 회원가입 버튼
                Button(
                    onClick = authViewModel::signUp, // 클릭 시 ViewModel의 signUp 함수 호출
                    enabled = isSignUpButtonEnabled, // 계산된 활성화 상태를 적용
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        // 활성화/비활성화 상태에 따라 다른 색상 적용
                        containerColor = ButtonActiveBackground,
                        contentColor = ButtonActiveText,
                        disabledContainerColor = ButtonDisabledBackground,
                        disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                    )
                ) {
                    // 로딩 상태에 따라 다른 텍스트 표시
                    Text(if (uiState.isLoading) "가입 중..." else "회원가입", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Context에서 Activity를 찾아 반환하는 확장 함수
 * Composable 내에서 현재 Activity에 접근해야 할 때
 * @return 찾은 Activity 객체, 못 찾으면 null을 반환
 */
private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this // 현재 Context가 Activity이면 바로 반환
    is ContextWrapper -> baseContext.findActivity() // Context가 다른 Context를 감싸고 있다면, 내부 Context에서 다시 탐색
    else -> null // Activity를 찾지 못한 경우
}
