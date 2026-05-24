package com.tcc.androidnative.core.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tcc.androidnative.R
import com.tcc.androidnative.ui.theme.LoginBrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isTimePickerVisible: Boolean,
    onTimePickerVisibilityChange: (Boolean) -> Unit,
    label: @Composable () -> Unit,
    fieldContentDescription: String,
    clockActionContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable (() -> Unit))? = null,
    dialogTestTag: String? = null
) {
    if (isTimePickerVisible) {
        val initialTime = remember(value.text) { parseTimeValue(value.text) }
        val timePickerState = rememberTimePickerState(
            initialHour = initialTime.first,
            initialMinute = initialTime.second,
            is24Hour = true
        )

        Dialog(onDismissRequest = { onTimePickerVisibilityChange(false) }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (dialogTestTag != null) Modifier.testTag(dialogTestTag) else Modifier),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        primary = LoginBrandBlue,
                        secondary = LoginBrandBlue,
                        tertiary = LoginBrandBlue
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        TimePicker(
                            state = timePickerState,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onTimePickerVisibilityChange(false) }) {
                                Text(
                                    text = stringResource(R.string.calendar_date_picker_cancel),
                                    color = LoginBrandBlue
                                )
                            }
                            TextButton(
                                onClick = {
                                    val formattedTime = formatPickedTime(
                                        hour = timePickerState.hour,
                                        minute = timePickerState.minute
                                    )
                                    onValueChange(
                                        TextFieldValue(
                                            text = formattedTime,
                                            selection = TextRange(formattedTime.length)
                                        )
                                    )
                                    onTimePickerVisibilityChange(false)
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.calendar_date_picker_confirm),
                                    color = LoginBrandBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        label = label,
        trailingIcon = {
            IconButton(
                onClick = { onTimePickerVisibilityChange(true) },
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = clockActionContentDescription
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = LoginBrandBlue
                )
            }
        },
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.semantics {
            contentDescription = fieldContentDescription
        }
    )
}

private fun parseTimeValue(value: String): Pair<Int, Int> {
    val pieces = value.split(":")
    val hour = pieces.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val minute = pieces.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}

private fun formatPickedTime(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour, minute)
}
