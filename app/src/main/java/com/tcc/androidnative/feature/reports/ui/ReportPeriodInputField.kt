package com.tcc.androidnative.feature.reports.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tcc.androidnative.R
import com.tcc.androidnative.core.util.DateFormats
import java.time.LocalDate

@Composable
internal fun ReportPeriodInputField(
    value: String,
    selectedStartDate: LocalDate?,
    selectedEndDate: LocalDate?,
    fieldDescription: String,
    calendarDescription: String,
    pickerTestTag: String,
    confirmButtonTag: String,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Periodo (dd/MM/yyyy)"
) {
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    if (isDatePickerVisible) {
        ReportPeriodPickerDialog(
            initialSelectedStartDate = selectedStartDate ?: LocalDate.now(),
            initialSelectedEndDate = selectedEndDate,
            pickerTestTag = pickerTestTag,
            confirmButtonTag = confirmButtonTag,
            onDismiss = { isDatePickerVisible = false },
            onConfirm = { startDate, endDate ->
                onPeriodSelected(startDate, endDate)
                isDatePickerVisible = false
            }
        )
    }

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
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
        modifier = modifier.semantics {
            contentDescription = fieldDescription
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportPeriodPickerDialog(
    initialSelectedStartDate: LocalDate,
    initialSelectedEndDate: LocalDate?,
    pickerTestTag: String,
    confirmButtonTag: String,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = DateFormats.toUtcMillis(initialSelectedStartDate),
        initialSelectedEndDateMillis = initialSelectedEndDate?.let(DateFormats::toUtcMillis),
        initialDisplayedMonthMillis = DateFormats.toUtcMillis(initialSelectedStartDate.withDayOfMonth(1))
    )
    val selectedStartDate = dateRangePickerState.selectedStartDateMillis?.let(DateFormats::fromUtcMillis)
    val selectedEndDate = dateRangePickerState.selectedEndDateMillis?.let(DateFormats::fromUtcMillis)
    val isConfirmEnabled = selectedStartDate != null &&
        selectedEndDate != null &&
        !selectedEndDate.isAfter(selectedStartDate.plusMonths(1))

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedStartDate != null && selectedEndDate != null) {
                        onConfirm(selectedStartDate, selectedEndDate)
                    }
                },
                enabled = isConfirmEnabled,
                modifier = Modifier.testTag(confirmButtonTag)
            ) {
                Text(stringResource(R.string.calendar_date_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.calendar_date_picker_cancel))
            }
        }
    ) {
        DateRangePicker(
            state = dateRangePickerState,
            title = {},
            showModeToggle = false,
            modifier = Modifier.testTag(pickerTestTag)
        )
    }
}
