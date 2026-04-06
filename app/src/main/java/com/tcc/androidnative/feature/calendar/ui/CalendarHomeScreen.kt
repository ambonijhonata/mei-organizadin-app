package com.tcc.androidnative.feature.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.ui.theme.BluePrimary
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val slotFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val localePtBr = Locale("pt", "BR")

@Composable
fun CalendarHomeScreen(
    viewModel: CalendarHomeViewModel = hiltViewModel(),
    onReauthenticateRequested: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CalendarHomeContent(
        uiState = uiState,
        onPreviousDay = viewModel::onPreviousDay,
        onNextDay = viewModel::onNextDay,
        onDateSelected = viewModel::onDateSelected,
        onReauthenticateRequested = onReauthenticateRequested
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarHomeContent(
    uiState: CalendarHomeUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onReauthenticateRequested: () -> Unit,
    listState: LazyListState? = null,
    currentLocalTimeProvider: () -> LocalTime = LocalTime::now,
    currentDateProvider: () -> LocalDate = { LocalDate.now(ZoneOffset.UTC) },
    onAutoFocusIndexResolved: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val resolvedListState = listState ?: rememberLazyListState()
    val timeSlots = remember { buildHalfHourSlots() }
    val slotLabels = remember(timeSlots) { timeSlots.map { it.format(slotFormatter) } }
    val eventsBySlot = remember(uiState.items) { buildSlotMap(uiState.items) }
    val weekdayLabel = remember(uiState.selectedDate) { formatWeekdayLabel(uiState.selectedDate) }
    val monthYearLabel = remember(uiState.selectedDate) { formatMonthYearLabel(uiState.selectedDate) }
    val datePickerContentDescription = stringResource(R.string.calendar_date_picker_open)
    val previousDayContentDescription = stringResource(R.string.calendar_previous_day)
    val nextDayContentDescription = stringResource(R.string.calendar_next_day)
    val headerContentDescription = stringResource(R.string.calendar_header_content_description)
    val reauthActionLabel = stringResource(R.string.feedback_calendar_reauth_action)
    val isDatePickerVisible = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.selectedDate, uiState.items, uiState.isLoading, uiState.errorMessage) {
        if (uiState.isLoading || uiState.errorMessage != null || slotLabels.isEmpty()) {
            return@LaunchedEffect
        }

        val targetIndex = resolveInitialFocusIndex(
            slotLabels = slotLabels,
            occupiedSlotLabels = eventsBySlot.keys,
            deviceLocalTime = currentLocalTimeProvider()
        )

        centerSlotOnViewport(resolvedListState, targetIndex)
        onAutoFocusIndexResolved(targetIndex)
    }

    if (isDatePickerVisible.value) {
        val selectedDateMillis = localDateToUtcMillis(uiState.selectedDate)
        val currentMonthMillis = firstDayOfCurrentMonthUtcMillis(currentDateProvider)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            initialDisplayedMonthMillis = currentMonthMillis
        )

        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDatePickerVisible.value = false
                        datePickerState.selectedDateMillis
                            ?.let(::utcMillisToLocalDate)
                            ?.let(onDateSelected)
                    }
                ) {
                    Text(stringResource(R.string.calendar_date_picker_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerVisible.value = false }) {
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = headerContentDescription },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = weekdayLabel,
                color = BluePrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.semantics { contentDescription = previousDayContentDescription },
                    onClick = onPreviousDay
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = BluePrimary
                    )
                }
                Text(
                    text = uiState.selectedDate.dayOfMonth.toString(),
                    color = Color.Black,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(
                    modifier = Modifier.semantics { contentDescription = nextDayContentDescription },
                    onClick = onNextDay
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BluePrimary
                    )
                }
            }
            Text(
                text = monthYearLabel,
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { isDatePickerVisible.value = true },
                border = BorderStroke(1.dp, Color(0xFFD1D5DB)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LoginBrandBlue),
                modifier = Modifier.semantics { contentDescription = datePickerContentDescription }
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = LoginBrandBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = DateFormats.toUiDate(uiState.selectedDate),
                    color = Color(0xFF374151)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.syncWarningMessage != null) {
            FeedbackMessageCard(message = uiState.syncWarningMessage)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.isRefreshing) {
            FeedbackMessageCard(
                text = stringResource(R.string.feedback_calendar_refreshing),
                tone = MessageTone.INFO
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.isReauthRequired) {
            Button(
                onClick = onReauthenticateRequested,
                modifier = Modifier.semantics {
                    contentDescription = reauthActionLabel
                }
            ) {
                Text(text = reauthActionLabel)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isLoading && uiState.items.isEmpty()) {
            FeedbackMessageCard(
                text = stringResource(R.string.feedback_calendar_loading),
                tone = MessageTone.INFO
            )
            return
        }

        if (uiState.errorMessage != null && uiState.items.isEmpty()) {
            FeedbackMessageCard(message = uiState.errorMessage)
            return
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            border = BorderStroke(1.dp, Color(0xFFE4E7EC)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                state = resolvedListState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(timeSlots) { index, slot ->
                    val label = slot.format(slotFormatter)
                    val item = eventsBySlot[label]

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (item == null) {
                            Text(
                                text = "$label - Livre",
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            AppointmentCard(
                                item = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        if (index < timeSlots.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F2F4))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(item: CalendarAgendaItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
        colors = CardDefaults.cardColors(containerColor = BluePrimary.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = item.timeLabel,
                color = BluePrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.Black,
                    style = MaterialTheme.typography.titleMedium
                )

                if (!item.serviceDescription.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = item.serviceDescription,
                            color = Color(0xFF6B7280),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = item.durationLabel,
                        color = Color(0xFF6B7280),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun buildHalfHourSlots(): List<LocalTime> {
    return (0 until 48).map { halfHourIndex ->
        LocalTime.MIDNIGHT.plusMinutes(halfHourIndex * 30L)
    }
}

internal fun resolveInitialFocusIndex(
    slotLabels: List<String>,
    occupiedSlotLabels: Set<String>,
    deviceLocalTime: LocalTime
): Int {
    if (slotLabels.isEmpty()) return 0

    val firstOccupiedIndex = slotLabels.indexOfFirst { label ->
        occupiedSlotLabels.contains(label)
    }
    if (firstOccupiedIndex >= 0) {
        return firstOccupiedIndex
    }

    val fallbackSlot = mapDeviceTimeToSlotLabel(deviceLocalTime)
    val fallbackIndex = slotLabels.indexOf(fallbackSlot)
    return if (fallbackIndex >= 0) fallbackIndex else 0
}

internal fun mapDeviceTimeToSlotLabel(deviceLocalTime: LocalTime): String {
    val minute = if (deviceLocalTime.minute < 30) 0 else 30
    return deviceLocalTime
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
        .format(slotFormatter)
}

internal fun firstDayOfCurrentMonthUtcMillis(currentDateProvider: () -> LocalDate): Long {
    return localDateToUtcMillis(currentDateProvider().withDayOfMonth(1))
}

internal fun utcMillisToLocalDate(utcMillis: Long): LocalDate {
    return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}

private fun localDateToUtcMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private suspend fun centerSlotOnViewport(listState: LazyListState, targetIndex: Int) {
    listState.scrollToItem(index = targetIndex)

    val layoutInfo = listState.layoutInfo
    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item -> item.index == targetIndex } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val targetCenter = targetItem.offset + (targetItem.size / 2f)
    listState.scrollBy(targetCenter - viewportCenter)
}

private fun buildSlotMap(items: List<CalendarAgendaItem>): Map<String, CalendarAgendaItem> {
    val bySlot = linkedMapOf<String, CalendarAgendaItem>()
    items.forEach { item ->
        bySlot.putIfAbsent(item.slotLabel, item)
    }
    return bySlot
}

private fun formatWeekdayLabel(date: LocalDate): String {
    val value = date.dayOfWeek.getDisplayName(TextStyle.FULL, localePtBr)
    return value.replaceFirstChar { current ->
        if (current.isLowerCase()) {
            current.titlecase(localePtBr)
        } else {
            current.toString()
        }
    }
}

private fun formatMonthYearLabel(date: LocalDate): String {
    val month = date.month.getDisplayName(TextStyle.FULL, localePtBr).replaceFirstChar { current ->
        if (current.isLowerCase()) {
            current.titlecase(localePtBr)
        } else {
            current.toString()
        }
    }
    return "$month De ${date.year}"
}
