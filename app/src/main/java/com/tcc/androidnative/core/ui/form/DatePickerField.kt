package com.tcc.androidnative.core.ui.form

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tcc.androidnative.R
import com.tcc.androidnative.core.util.DateFormats
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    isDatePickerVisible: Boolean,
    onDatePickerVisibilityChange: (Boolean) -> Unit,
    label: String,
    fieldContentDescription: String,
    calendarActionContentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    supportingText: (@Composable (() -> Unit))? = null
) {
    if (isDatePickerVisible) {
        val selectedDateMillis = DateFormats.toUtcMillis(selectedDate)
        val displayedMonthMillis = DateFormats.toUtcMillis(selectedDate.withDayOfMonth(1))
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            initialDisplayedMonthMillis = displayedMonthMillis
        )
        DatePickerDialog(
            onDismissRequest = { onDatePickerVisibilityChange(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(DateFormats::fromUtcMillis)
                            ?.let(onDateSelected)
                        onDatePickerVisibilityChange(false)
                    }
                ) {
                    Text(stringResource(R.string.calendar_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDatePickerVisibilityChange(false) }) {
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
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(
                onClick = { onDatePickerVisibilityChange(true) },
                enabled = enabled,
                modifier = Modifier.semantics {
                    contentDescription = calendarActionContentDescription
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null
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
