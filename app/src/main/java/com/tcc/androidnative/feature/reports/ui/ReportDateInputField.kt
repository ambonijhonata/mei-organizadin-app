package com.tcc.androidnative.feature.reports.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.res.stringResource
import com.tcc.androidnative.R
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.core.util.InputMasks
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportDateInputField(
    value: String,
    label: String,
    fieldDescription: String,
    calendarDescription: String,
    onValueChange: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }
    val fieldValue = remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(value) {
        if (fieldValue.value.text != value) {
            fieldValue.value = textFieldValueAtEnd(value)
        }
    }

    if (isDatePickerVisible) {
        val selectedDate = runCatching { DateFormats.parseUiDate(value) }.getOrNull() ?: LocalDate.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateFormats.toUtcMillis(selectedDate),
            initialDisplayedMonthMillis = DateFormats.toUtcMillis(selectedDate.withDayOfMonth(1))
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(DateFormats::fromUtcMillis)
                            ?.let(onDateSelected)
                        isDatePickerVisible = false
                    }
                ) {
                    Text(stringResource(R.string.calendar_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerVisible = false }) {
                    Text(stringResource(R.string.calendar_date_picker_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    OutlinedTextField(
        value = fieldValue.value,
        onValueChange = { incoming ->
            fieldValue.value = maskedFieldValue(incoming)
            onValueChange(fieldValue.value.text)
        },
        label = { Text(label) },
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = { isDatePickerVisible = true },
                modifier = Modifier.semantics {
                    contentDescription = calendarDescription
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.semantics {
            contentDescription = fieldDescription
        }
    )
}

private fun textFieldValueAtEnd(text: String): TextFieldValue {
    return TextFieldValue(text = text, selection = TextRange(text.length))
}

private fun maskedFieldValue(incoming: TextFieldValue): TextFieldValue {
    val formatted = InputMasks.formatBirthDateInput(incoming.text)
    return TextFieldValue(text = formatted, selection = TextRange(formatted.length))
}
