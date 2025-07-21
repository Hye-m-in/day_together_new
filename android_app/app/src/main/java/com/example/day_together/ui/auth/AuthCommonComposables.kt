package com.example.day_together.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day_together.ui.theme.ErrorRed
import com.example.day_together.ui.theme.ScreenBackground
import com.example.day_together.ui.theme.TextPrimary

/**
 * 인증(로그인, 회원가입 등) 화면들에서 공통으로 사용되는 UI 컴포저블 함수들을 모아둠
 */

/**
 * 계정 찾기 화면에서 사용하는 기본 텍스트 필드 컴포저블
 * @param label 필드 상단에 표시될 라벨 텍스트
 * @param value 텍스트 필드의 현재 값
 * @param onValueChange 값이 변경될 때 호출되는 콜백
 * @param imeAction 키보드 액션 버튼 설정 (예: 다음, 완료)
 * @param focusManager 키보드 액션 처리를 위한 포커스 매니저
 * @param onDone '완료' 액션 시 호출될 선택적 콜백
 */
@Composable
fun FindAccountTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction,
    focusManager: FocusManager, onDone: (() -> Unit)? = null, placeholder: String? = null
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp)) {
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
            modifier = Modifier.fillMaxWidth(),
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

// '기타' 가족 관계 입력의 최대 글자 수
const val MAX_FAMILY_MEMBER_OTHER_LENGTH = 10

/**
 * 회원가입 화면에서 사용하는 텍스트 필드. 에러 메시지 표시 기능이 포함
 * @param error 표시할 에러 메시지. null이 아닐 경우 에러 상태로 UI가 변경됨
 * @param trailingIcon 필드 끝에 표시될 아이콘 (비밀번호 보기/숨기기 버튼)
 */
@Composable
fun SignUpTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    placeholder: String? = null, keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    imeAction: ImeAction,
    focusManager: FocusManager, onDone: (() -> Unit)? = null,
    error: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontSize = 15.sp),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { onDone?.invoke() ?: focusManager.clearFocus() }
            ),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if(error != null) ErrorRed else MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if(error != null) ErrorRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                cursorColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = ErrorRed,
                errorSupportingTextColor = ErrorRed
            ),
            isError = error != null,
            supportingText = {
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            trailingIcon = trailingIcon
        )
    }
}

/**
 * 양력/음력 선택에 사용되는 체크박스 컴포저블
 * @param text 체크박스 옆에 표시될 텍스트(양력/음력)
 * @param checked 체크 상태
 * @param onCheckedChange 체크 상태 변경 시 호출되는 콜백
 */
@Composable
fun SolarLunarCheckbox(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextPrimary.copy(alpha = 0.7f),
                checkmarkColor = ScreenBackground
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = TextPrimary
        )
    }
}

/**
 * 가족 구성원(역할)을 선택하는 UI 컴포저블. '기타'의 직접 입력 기능 포함
 * @param title 컴포넌트 상단에 표시될 제목
 * @param members 선택 가능한 기본 구성원 목록
 * @param selections 각 구성원의 선택 상태 Map
 * @param onSelectionChange 구성원 선택 상태 변경 시 호출되는 콜백
 * @param otherChecked '기타' 항목 체크 여부
 * @param onOtherCheckedChange '기타' 항목 체크 상태 변경 시 호출되는 콜백
 * @param otherText '기타' 항목에 입력된 텍스트 값
 * @param onOtherTextChange '기타' 항목 텍스트 변경 시 호출되는 콜백
 */
@Composable
fun FamilyMemberSelection(
    title: String,
    members: List<String>,
    selections: Map<String, Boolean>,
    onSelectionChange: (String, Boolean) -> Unit,
    otherChecked: Boolean,
    onOtherCheckedChange: (Boolean) -> Unit,
    otherText: String,
    onOtherTextChange: (String) -> Unit,
    focusManager: FocusManager
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 그리드 레이아웃을 위해 멤버 목록을 나눔
                val firstRowMembers = members.take(4)
                val secondRowMembers = members.drop(4).take(2)

                Row(modifier = Modifier.fillMaxWidth()) {
                    firstRowMembers.forEach { member ->
                        FamilyMemberItem(
                            text = member,
                            selected = selections[member] ?: false,
                            onSelectedChange = { onSelectionChange(member, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    secondRowMembers.forEach { member ->
                        FamilyMemberItem(
                            text = member,
                            selected = selections[member] ?: false,
                            onSelectedChange = { onSelectionChange(member, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 남은 공간을 '기타' 항목이 채우도록 가중치 계산
                    val otherItemWeight = (4 - secondRowMembers.size).toFloat().coerceAtLeast(1f)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(otherItemWeight)
                            .clickable { onOtherCheckedChange(!otherChecked) }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        Checkbox(
                            checked = otherChecked,
                            onCheckedChange = onOtherCheckedChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = TextPrimary.copy(alpha = 0.6f),
                                checkmarkColor = ScreenBackground
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text("기타", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = TextPrimary)
                        Spacer(Modifier.width(4.dp))
                        // '기타' 텍스트를 직접 입력하는 필드
                        BasicTextField(
                            value = otherText,
                            onValueChange = {
                                if (it.length <= MAX_FAMILY_MEMBER_OTHER_LENGTH) {
                                    onOtherTextChange(it)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .background(
                                    color = if (otherChecked) ScreenBackground.copy(alpha = 0.5f) else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(
                                    BorderStroke(0.5.dp, if (otherChecked) TextPrimary.copy(alpha = 0.4f) else Color.Transparent),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontSize = 15.sp),
                            enabled = otherChecked,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }
        }
    }
}

/**
 * `FamilyMemberSelection` 내에서 사용되는 개별 가족 구성원 박스 (가족구성원 역할 텍스트 +  텍스트)
 */
@Composable
private fun FamilyMemberItem(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onSelectedChange(!selected) }
            .padding(vertical = 2.dp, horizontal = 2.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onSelectedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = TextPrimary.copy(alpha = 0.6f),
                checkmarkColor = ScreenBackground
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = TextPrimary)
    }
}