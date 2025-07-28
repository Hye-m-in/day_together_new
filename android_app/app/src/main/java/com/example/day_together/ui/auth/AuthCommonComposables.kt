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

const val MAX_FAMILY_MEMBER_OTHER_LENGTH = 10

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

@Composable
fun FamilyMemberItem(
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