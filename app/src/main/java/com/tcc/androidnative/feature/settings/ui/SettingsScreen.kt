package com.tcc.androidnative.feature.settings.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.R
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private const val TOOLTIP_VISIBLE_MILLIS = 10_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onInitialSetupCompleted: () -> Unit = {},
    onNavigateHome: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = uiState.requiresFirstAccessSave) { }

    if (isDatePickerVisible) {
        val selectedDateMillis = localDateToUtcMillis(uiState.startDate)
        val displayedMonthMillis = localDateToUtcMillis(uiState.startDate.withDayOfMonth(1))
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            initialDisplayedMonthMillis = displayedMonthMillis
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(::utcMillisToLocalDate)
                            ?.let(viewModel::onStartDateSelected)
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

    SettingsContent(
        uiState = uiState,
        onUseStartDateFilterChanged = viewModel::onUseStartDateFilterChanged,
        onDateFieldClick = { isDatePickerVisible = true },
        onTooltipClick = viewModel::showTooltip,
        onTooltipClose = viewModel::hideTooltip,
        onSaveClick = {
            val wasFirstAccess = viewModel.save()
            if (wasFirstAccess) {
                onInitialSetupCompleted()
            }
        },
        onCancelClick = {
            if (viewModel.cancel()) {
                onNavigateHome()
            }
        }
    )
}

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onUseStartDateFilterChanged: (Boolean) -> Unit,
    onDateFieldClick: () -> Unit,
    onTooltipClick: () -> Unit,
    onTooltipClose: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    tooltipAutoDismissMillis: Long = TOOLTIP_VISIBLE_MILLIS
) {
    val cardShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val checkboxDescription = stringResource(R.string.settings_enable_start_date_checkbox_description)
    val tooltipButtonDescription = stringResource(R.string.settings_tooltip_button_description)
    val tooltipCloseDescription = stringResource(R.string.settings_tooltip_close_button_description)
    val startDateFieldDescription = stringResource(R.string.settings_start_date_field_description)
    val saveButtonDescription = stringResource(R.string.settings_save_button_description)
    val cancelButtonDescription = stringResource(R.string.settings_cancel_button_description)

    LaunchedEffect(uiState.isTooltipVisible) {
        if (uiState.isTooltipVisible) {
            kotlinx.coroutines.delay(tooltipAutoDismissMillis)
            onTooltipClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DrawerMenuIconBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (uiState.requiresFirstAccessSave) {
                    Text(
                        text = stringResource(R.string.settings_first_access_save_required),
                        color = Color(0xFFB45309),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        modifier = Modifier.semantics {
                            contentDescription = checkboxDescription
                        },
                        checked = uiState.useStartDateFilter,
                        onCheckedChange = onUseStartDateFilterChanged
                    )
                    Text(
                        text = stringResource(R.string.settings_enable_start_date_checkbox),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_start_date_label),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        modifier = Modifier.semantics {
                            contentDescription = tooltipButtonDescription
                        },
                        onClick = onTooltipClick
                    ) {
                        Text("?")
                    }
                }

                if (uiState.isTooltipVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.settings_start_date_tooltip_message),
                                color = Color(0xFF1E3A8A),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                modifier = Modifier.semantics {
                                    contentDescription = tooltipCloseDescription
                                },
                                onClick = onTooltipClose
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = DateFormats.toUiDate(uiState.startDate),
                    onValueChange = {},
                    enabled = uiState.useStartDateFilter,
                    readOnly = true,
                    label = { Text(stringResource(R.string.settings_start_date_field_label)) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = startDateFieldDescription
                        }
                        .clickable(
                            enabled = uiState.useStartDateFilter,
                            interactionSource = interactionSource,
                            indication = null
                        ) { onDateFieldClick() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onSaveClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginBrandBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = saveButtonDescription
                        }
                    ) {
                        Text(stringResource(R.string.settings_save_button))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCancelClick,
                        enabled = !uiState.requiresFirstAccessSave,
                        modifier = Modifier.semantics {
                            contentDescription = cancelButtonDescription
                        }
                    ) {
                        Text(stringResource(R.string.settings_cancel_button))
                    }
                }
            }
        }
    }
}

internal fun utcMillisToLocalDate(utcMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}

private fun localDateToUtcMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
