package com.example.day_together.ui.settings

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.day_together.R
import com.example.day_together.ui.auth.AuthViewModel
import com.example.day_together.ui.auth.FamilyMemberSelection
import com.example.day_together.ui.theme.*
import com.google.firebase.storage.FirebaseStorage

/**
 * 개인정보 수정 화면의 UI를 구성하는 메인 컴포저블 함수
 * 모든 데이터와 로직은 EditProfileViewModel을 통해 관리
 *
 * @param navController 화면 이동을 제어하는 NavController
 * @param viewModel UI 상태와 비즈니스 로직을 담당하는 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = viewModel()

) {




    // ViewModel의 uiState를 구독하여 상태 변경 시 자동으로 UI를 업데이트
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // 탈퇴 확인 다이얼로그 표시 여부 상태
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isProfileImageChanged = uiState.newProfileImageUri != null

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.onProfileImageChanged(uri)
            }
        }
    )


    // 회원탈퇴 성공 시 로그인 화면으로 이동 (앱 재시작 효과)
    LaunchedEffect(key1 = uiState.isDeleteSuccess) {
        if (uiState.isDeleteSuccess) {
            Toast.makeText(context, "회원탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()

            // 네비게이션 스택을 비우고 로그인 화면으로 이동
            navController.navigate("login") {
                popUpTo(0) { inclusive = true } // 백스택 모두 제거
            }
        }
    }

    // 1. 부가 효과(Side Effect) 처리
    // 사용자에게 보여줄 메시지(Toast)나 화면 이동 같은 일회성 이벤트 처리
    // 저장 성공 여부를 감지하여 이전 화면으로 돌아감
    LaunchedEffect(key1 = uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            // isSaveSuccess가 true가 되면 이전 화면으로 이동
            navController.popBackStack()
        }
    }

    // 사용자에게 보여줄 메시지(에러, 성공 등)를 감지하여 Toast로 표시
    LaunchedEffect(key1 = uiState.userMessage) {
        uiState.userMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // 메시지를 한 번 표시한 후에는 ViewModel의 상태를 초기화하여 중복 표시를 방지
            viewModel.userMessageShown()
        }
    }

    // 2. UI 로직 및 상태 계산
    // '완료' 버튼의 활성화 여부를 계산 -> 이 로직은 ViewModel의 상태에 따라 결정
    val isCompleteButtonEnabled = !uiState.isLoading &&
            uiState.nameError == null &&
            uiState.passwordError == null
            // 로딩 중이 아닐 때
  //          (uiState.nameInput.isNotBlank() && uiState.birthDateInput.length == 8) && // 필수 정보 유효성
                    // 정보가 변경되었거나
                    //(
//                            isProfileImageChanged || uiState.nameInput != uiState.user?.name ||
//                            uiState.birthDateInput != uiState.user?.birthDate ||
//                            uiState.positionInput != uiState.user?.position) ||
//                            // 혹은 유효한 비밀번호 변경 시도가 있을 때
//                            (uiState.oldPasswordInput.isNotBlank() && uiState.newPasswordInput.length >= 8 && uiState.newPasswordInput == uiState.confirmNewPasswordInput)
//                      )


    // 3. UI 레이아웃 구성
    Day_togetherTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("개인정보 수정", style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
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
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 로딩 중일 경우 로딩 인디케이터를 표시
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    val profileImageModel = uiState.newProfileImageUri ?: if (uiState.profile_image.isNotBlank())
                        uiState.profile_image else R.drawable.ic_add_photo

                    Log.d("EditProfile", "Profile URL: ${uiState.user?.profile_image}")
                    AsyncImage(
                        model = profileImageModel,
                        contentDescription = "프로필 이미지",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .clickable { galleryLauncher.launch("image/*")
                            },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // 이름 입력 필드
                    EditProfileTextField(
                        label = "이름",
                        value = uiState.nameInput,
                        onValueChange = viewModel::onNameChange,
                        imeAction = ImeAction.Next,
                        focusManager = focusManager,
                        error = uiState.nameError
                    )

                    // 생년월일 섹션 (읽기 전용으로)
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        // Row를 Text 라벨로 변경
                        Text("생년월일", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp), color = TextPrimary, modifier = Modifier.padding(end = 12.dp))


                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = uiState.birthDateInput,
                            onValueChange = { /* 수정 불가 */ },
                            readOnly = true,
                            enabled = false,
                            placeholder = { Text("ex)20040506", color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = uiState.birthDateError != null,
                            supportingText = {
                                uiState.birthDateError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary.copy(alpha = 0.7f), fontSize = 15.sp), // [수정] 비활성화 텍스트 색상
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), // [수정]
                                disabledTextColor = TextPrimary.copy(alpha = 0.7f) // [수정]
                            )
                        )
                    }

                    // 이메일(ID) 필드 (읽기 전용)
                    EditProfileTextField(
                        label = "ID(Email)",
                        value = uiState.user?.email ?: "",
                        onValueChange = {},
                        imeAction = ImeAction.Next,
                        focusManager = focusManager,
                        readOnly = true
                    )

                    // 비밀번호 변경 관련 필드
                    EditProfileTextField(label = "기존 Password", value = uiState.oldPasswordInput, onValueChange = viewModel::onOldPasswordChange, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Next, focusManager = focusManager, placeholder = "변경 시에만 입력", error = uiState.passwordError)
                    EditProfileTextField(label = "변경할 Password", value = uiState.newPasswordInput, onValueChange = viewModel::onNewPasswordChange, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Next, focusManager = focusManager, placeholder = "영문,숫자,특수기호 포함 8자 이상")
                    EditProfileTextField(label = "변경할 Password 확인", value = uiState.confirmNewPasswordInput, onValueChange = viewModel::onConfirmNewPasswordChange, keyboardType = KeyboardType.Password, isPassword = true, imeAction = ImeAction.Done, focusManager = focusManager, onDone = { focusManager.clearFocus() })

                    Spacer(modifier = Modifier.height(24.dp))

                    // 가족 구성원 선택 (ViewModel과 연결)
                    FamilyMemberSelection(
                        title = "가족 구성원 중 나는?",
                        members = listOf("할아버지", "할머니", "아버지", "어머니", "아들", "딸"),
                        selections = uiState.familyMemberSelections,
                        onSelectionChange = viewModel::onFamilyMemberSelectionChange,
                        otherChecked = uiState.otherFamilyMemberChecked,
                        onOtherCheckedChange = viewModel::onOtherFamilyMemberCheckedChange,
                        otherText = uiState.otherFamilyMemberText,
                        onOtherTextChange = viewModel::onOtherFamilyMemberTextChange,
                        focusManager = focusManager
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 완료 버튼
                    Button(
                        onClick = viewModel::onSaveClicked,
                        enabled = isCompleteButtonEnabled,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonActiveBackground,
                            contentColor = ButtonActiveText,
                            disabledContainerColor = ButtonDisabledBackground,
                            disabledContentColor = TextPrimary.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("완료", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 중복 제거 및 기능 연결된 회원탈퇴 버튼
                    TextButton(
                        onClick = { showDeleteDialog = true }
                    ) {
                        Text(
                            "회원탈퇴",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = TextDecoration.Underline,
                                color = TextPrimary.copy(alpha = 0.7f)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 회원탈퇴 확인 팝업 (AlertDialog)
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false }, // 바깥 클릭 시 닫기
                    title = { Text(text = "회원 탈퇴") },
                    text = { Text(text = "정말로 탈퇴하시겠습니까?\n탈퇴 시 모든 정보가 삭제되며 복구할 수 없습니다.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                viewModel.onDeleteAccountConfirmed() // 뷰모델에 삭제 요청
                            }
                        ) {
                            Text("확인", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteDialog = false } // 취소 시 다이얼로그 닫기
                        ) {
                            Text("취소")
                        }
                    },
                    containerColor = ScreenBackground,
                    textContentColor = TextPrimary,
                    titleContentColor = TextPrimary
                )
            }
        }
    }
}





// 하위 컴포저블

@Composable
private fun EditProfileTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String? = null, keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false, imeAction: ImeAction,
    focusManager: FocusManager, onDone: (() -> Unit)? = null,
    readOnly: Boolean = false, error: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp), color = TextPrimary, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            placeholder = { if (placeholder != null) Text(placeholder, color = TextPrimary.copy(alpha = 0.6f), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = if (readOnly) TextPrimary.copy(alpha = 0.7f) else TextPrimary, fontSize = 15.sp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { onDone?.invoke() ?: focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(8.dp),
            isError = error != null,
            supportingText = {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            enabled = !readOnly
        )
    }
}

@Composable
private fun SolarLunarCheckbox(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onCheckedChange(true) }) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.size(20.dp), colors = CheckboxDefaults.colors())
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = TextPrimary)
    }
}
