package com.tcc.androidnative.feature.calendar.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.calendar.data.CalendarPaymentStatus
import com.tcc.androidnative.ui.theme.BluePrimary
import com.tcc.androidnative.ui.theme.GreenSuccess
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val slotFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val localePtBr = Locale("pt", "BR")
private val agendaSlotHeight = 24.dp
private const val agendaSlotMinutes = 15L
private const val agendaHorizontalSwipeThresholdFraction = 0.25f
private const val agendaHorizontalSwipeThresholdMinPx = 72f

@Composable
fun CalendarHomeScreen(
    viewModel: CalendarHomeViewModel = hiltViewModel(),
    onReauthenticateRequested: () -> Unit = {},
    onAppointmentClick: (CalendarAgendaItem) -> Unit = {},
    onEmptySlotClick: (LocalDate, LocalTime) -> Unit = { _, _ -> },
    listState: LazyListState? = null,
    allowInitialAutoFocus: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var nextAutoFocusRequestId by rememberSaveable { mutableIntStateOf(0) }
    var pendingAutoFocusRequestId by rememberSaveable { mutableStateOf<Int?>(null) }

    CalendarHomeContent(
        uiState = uiState,
        onPreviousDay = {
            nextAutoFocusRequestId += 1
            pendingAutoFocusRequestId = nextAutoFocusRequestId
            viewModel.onPreviousDay()
        },
        onNextDay = {
            nextAutoFocusRequestId += 1
            pendingAutoFocusRequestId = nextAutoFocusRequestId
            viewModel.onNextDay()
        },
        onDateSelected = { date ->
            nextAutoFocusRequestId += 1
            pendingAutoFocusRequestId = nextAutoFocusRequestId
            viewModel.onDateSelected(date)
        },
        onReauthenticateRequested = onReauthenticateRequested,
        onSyncRequested = viewModel::synchronizeNow,
        onAppointmentClick = onAppointmentClick,
        onEmptySlotClick = onEmptySlotClick,
        listState = listState,
        allowInitialAutoFocus = allowInitialAutoFocus,
        autoFocusRequestId = pendingAutoFocusRequestId,
        onAutoFocusRequestConsumed = { pendingAutoFocusRequestId = null }
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
    modifier: Modifier = Modifier,
    onSyncRequested: () -> Unit = {},
    onAppointmentClick: (CalendarAgendaItem) -> Unit = {},
    onEmptySlotClick: (LocalDate, LocalTime) -> Unit = { _, _ -> },
    listState: LazyListState? = null,
    currentLocalTimeProvider: () -> LocalTime = LocalTime::now,
    currentDateProvider: () -> LocalDate = { LocalDate.now(calendarZone) },
    onAutoFocusIndexResolved: (Int) -> Unit = {},
    allowInitialAutoFocus: Boolean = true,
    autoFocusRequestId: Int? = null,
    onAutoFocusRequestConsumed: () -> Unit = {}
) {
    val resolvedListState = listState ?: rememberLazyListState()
    val timeSlots = remember { buildQuarterHourSlots() }
    val slotLabels = remember(timeSlots) { timeSlots.map { it.format(slotFormatter) } }
    val eventsBySlot = remember(uiState.items) { buildSlotMap(uiState.items) }
    val agendaRows = remember(timeSlots, uiState.items) { buildAgendaRows(timeSlots, uiState.items) }
    val weekdayLabel = remember(uiState.selectedDate) { formatWeekdayLabel(uiState.selectedDate) }
    val monthYearLabel = remember(uiState.selectedDate) { formatMonthYearLabel(uiState.selectedDate) }
    val datePickerContentDescription = stringResource(R.string.calendar_date_picker_open)
    val previousDayContentDescription = stringResource(R.string.calendar_previous_day)
    val nextDayContentDescription = stringResource(R.string.calendar_next_day)
    val headerContentDescription = stringResource(R.string.calendar_header_content_description)
    val reauthActionLabel = stringResource(R.string.feedback_calendar_reauth_action)
    val lastSyncLabel = stringResource(R.string.calendar_last_sync_label, uiState.lastSyncDisplayValue)
    val lastSyncRefreshDescription = stringResource(R.string.calendar_last_sync_refresh)
    val isDatePickerVisible = rememberSaveable { mutableStateOf(false) }
    var initialAutoFocusConsumed by rememberSaveable { mutableStateOf(false) }
    var consumedAutoFocusRequestId by rememberSaveable { mutableStateOf<Int?>(null) }
    var agendaContainerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(
        allowInitialAutoFocus,
        autoFocusRequestId,
        uiState.selectedDate,
        uiState.items,
        uiState.isLoading,
        uiState.errorMessage
    ) {
        if (uiState.isLoading || uiState.errorMessage != null || slotLabels.isEmpty()) {
            return@LaunchedEffect
        }

        val hasNewFocusRequest = autoFocusRequestId != null && autoFocusRequestId != consumedAutoFocusRequestId
        val shouldRunInitialAutoFocus = allowInitialAutoFocus && !initialAutoFocusConsumed && autoFocusRequestId == null
        if (!hasNewFocusRequest && !shouldRunInitialAutoFocus) {
            return@LaunchedEffect
        }

        val targetIndex = resolveInitialFocusIndex(
            slotLabels = slotLabels,
            occupiedSlotLabels = eventsBySlot.keys,
            deviceLocalTime = currentLocalTimeProvider()
        )

        centerSlotOnViewport(resolvedListState, targetIndex)
        onAutoFocusIndexResolved(targetIndex)
        if (hasNewFocusRequest) {
            consumedAutoFocusRequestId = autoFocusRequestId
            onAutoFocusRequestConsumed()
        }
        if (shouldRunInitialAutoFocus) {
            initialAutoFocusConsumed = true
        }
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
                .testTag("calendar_header_container")
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_last_sync_strip"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = lastSyncLabel,
                color = Color(0xFF4B5563),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .testTag("calendar_last_sync_label")
            )
            IconButton(
                onClick = onSyncRequested,
                enabled = !uiState.isRefreshing,
                modifier = Modifier
                    .testTag("calendar_last_sync_refresh")
                    .semantics { contentDescription = lastSyncRefreshDescription }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = LoginBrandBlue
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
                .testTag("calendar_agenda_container")
                .weight(1f)
                .onSizeChanged { agendaContainerSize = it }
                .pointerInput(onPreviousDay, onNextDay, agendaContainerSize) {
                    var accumulatedHorizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { accumulatedHorizontalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            accumulatedHorizontalDrag += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            val threshold = resolveAgendaSwipeThresholdPx(agendaContainerSize.width)
                            when {
                                accumulatedHorizontalDrag >= threshold -> onPreviousDay()
                                accumulatedHorizontalDrag <= -threshold -> onNextDay()
                            }
                            accumulatedHorizontalDrag = 0f
                        },
                        onDragCancel = {
                            accumulatedHorizontalDrag = 0f
                        }
                    )
                }
                .semantics { contentDescription = "Agenda do dia" },
            border = BorderStroke(1.dp, Color(0xFFE4E7EC)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                state = resolvedListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("calendar_agenda_list")
            ) {
                itemsIndexed(agendaRows) { index, row ->
                    val slot = row.slot
                    val label = slot.format(slotFormatter)
                    val item = row.item

                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (item == null) {
                            Text(
                                text = "$label - Livre",
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEmptySlotClick(uiState.selectedDate, slot) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("calendar-empty-slot-$label"),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            AppointmentCard(
                                item = item,
                                onClick = { onAppointmentClick(item) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = (row.occupiedSlotCount * agendaSlotHeight.value).dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        if (index < agendaRows.lastIndex) {
                            HorizontalDivider(color = Color(0xFFF1F2F4))
                        }
                    }
                }
            }
        }
    }
}

private fun resolveAgendaSwipeThresholdPx(containerWidthPx: Int): Float {
    val proportionalThreshold = containerWidthPx * agendaHorizontalSwipeThresholdFraction
    return maxOf(agendaHorizontalSwipeThresholdMinPx, proportionalThreshold)
}

@Composable
private fun AppointmentCard(item: CalendarAgendaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val appointmentColors = when (item.paymentStatus) {
        CalendarPaymentStatus.NONE -> AppointmentColors(
            border = BluePrimary.copy(alpha = 0.25f),
            background = BluePrimary.copy(alpha = 0.10f),
            accent = BluePrimary
        )
        CalendarPaymentStatus.PARTIAL -> AppointmentColors(
            border = Color(0xFFF0C553).copy(alpha = 0.35f),
            background = Color(0xFFFFF7DA),
            accent = Color(0xFFB7791F)
        )
        CalendarPaymentStatus.PAID -> AppointmentColors(
            border = GreenSuccess.copy(alpha = 0.25f),
            background = GreenSuccess.copy(alpha = 0.10f),
            accent = GreenSuccess
        )
    }

    Card(
        modifier = modifier
            .testTag("appointment_card_${item.eventId}")
            .clickable(onClick = onClick),
        border = BorderStroke(1.dp, appointmentColors.border),
        colors = CardDefaults.cardColors(containerColor = appointmentColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            TimeRangeLabel(
                timeLabel = item.timeLabel,
                color = appointmentColors.accent
            )
            Text(
                text = item.title,
                color = Color.Black,
                style = MaterialTheme.typography.titleMedium
            )

            if (!item.serviceDescription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = item.serviceDescription,
                        color = Color(0xFF6B7280),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier
                        .size(14.dp)
                        .padding(end = 4.dp)
                )
                Text(
                    text = item.durationLabel,
                    color = Color(0xFF6B7280),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TimeRangeLabel(timeLabel: String, color: Color) {
    val (startLabel, endLabel) = splitTimeRangeLabel(timeLabel)
    val label = endLabel?.let { "$startLabel - $it" } ?: startLabel
    Text(text = label, color = color, style = MaterialTheme.typography.bodySmall)
}

internal fun splitTimeRangeLabel(timeLabel: String): Pair<String, String?> {
    val separator = " - "
    val separatorIndex = timeLabel.indexOf(separator)
    if (separatorIndex < 0) return timeLabel to null
    return timeLabel.substring(0, separatorIndex) to
        timeLabel.substring(separatorIndex + separator.length).takeIf { it.isNotBlank() }
}

private data class AppointmentColors(val border: Color, val background: Color, val accent: Color)

private fun buildQuarterHourSlots(): List<LocalTime> = (0 until 96).map { quarterHourIndex ->
    LocalTime.MIDNIGHT.plusMinutes(quarterHourIndex * agendaSlotMinutes)
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
    val minute = (deviceLocalTime.minute / agendaSlotMinutes * agendaSlotMinutes).toInt()
    return deviceLocalTime
        .withMinute(minute)
        .withSecond(0)
        .withNano(0)
        .format(slotFormatter)
}

internal fun firstDayOfCurrentMonthUtcMillis(currentDateProvider: () -> LocalDate): Long =
    localDateToUtcMillis(currentDateProvider().withDayOfMonth(1))

internal fun utcMillisToLocalDate(utcMillis: Long): LocalDate =
    Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

private fun localDateToUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private suspend fun centerSlotOnViewport(listState: LazyListState, targetIndex: Int) {
    listState.scrollToItem(index = targetIndex)

    val layoutInfo = listState.layoutInfo
    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item -> item.index == targetIndex } ?: return
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val targetCenter = targetItem.offset + (targetItem.size / 2f)
    listState.scrollBy(targetCenter - viewportCenter)
}

internal fun buildSlotMap(items: List<CalendarAgendaItem>, zone: ZoneId = calendarZone): Map<String, CalendarAgendaItem> {
    val bySlot = linkedMapOf<String, CalendarAgendaItem>()
    items.forEach { item ->
        val eventEnd = item.eventEnd
        if (eventEnd == null || !eventEnd.isAfter(item.eventStart)) {
            bySlot.putIfAbsent(item.slotLabel, item)
            return@forEach
        }
        val eventStartAtZone = item.eventStart.atZone(zone)
        val eventLocalDate = eventStartAtZone.toLocalDate()
        var slotStart = eventStartAtZone.withMinute(
            (eventStartAtZone.minute / agendaSlotMinutes * agendaSlotMinutes).toInt()
        )
            .withSecond(0).withNano(0)
        while (slotStart.toInstant().isBefore(eventEnd) && slotStart.toLocalDate() == eventLocalDate) {
            bySlot.putIfAbsent(slotStart.format(slotFormatter), item)
            slotStart = slotStart.plusMinutes(agendaSlotMinutes)
        }
    }
    return bySlot
}

internal data class CalendarAgendaRow(val slot: LocalTime, val item: CalendarAgendaItem? = null, val occupiedSlotCount: Int = 1)

internal fun buildAgendaRows(timeSlots: List<LocalTime>, items: List<CalendarAgendaItem>, zone: ZoneId = calendarZone): List<CalendarAgendaRow> {
    val itemsByInitialSlot = linkedMapOf<String, CalendarAgendaItem>()
    items.forEach { item -> itemsByInitialSlot.putIfAbsent(item.slotLabel, item) }
    val rows = mutableListOf<CalendarAgendaRow>()
    val slotDurationMinutes = timeSlots
        .zipWithNext()
        .firstOrNull()
        ?.let { (first, second) -> java.time.Duration.between(first, second).toMinutes() }
        ?.takeIf { it > 0L }
        ?: agendaSlotMinutes
    var slotIndex = 0
    while (slotIndex < timeSlots.size) {
        val slot = timeSlots[slotIndex]
        val item = itemsByInitialSlot[slot.format(slotFormatter)]
        if (item == null) {
            rows += CalendarAgendaRow(slot = slot)
            slotIndex += 1
            continue
        }
        val occupiedSlotCount = occupiedSlotCount(item, slotDurationMinutes, zone)
            .coerceAtMost(timeSlots.size - slotIndex)
        rows += CalendarAgendaRow(slot = slot, item = item, occupiedSlotCount = occupiedSlotCount)
        slotIndex += occupiedSlotCount
    }
    return rows
}

private fun occupiedSlotCount(item: CalendarAgendaItem, slotDurationMinutes: Long, zone: ZoneId): Int {
    val eventEnd = item.eventEnd ?: return 1
    if (!eventEnd.isAfter(item.eventStart)) return 1
    val eventStartAtZone = item.eventStart.atZone(zone)
    var slotStart = eventStartAtZone.withMinute(
        (eventStartAtZone.minute / slotDurationMinutes * slotDurationMinutes).toInt()
    )
        .withSecond(0).withNano(0)
    var count = 0
    while (slotStart.toInstant().isBefore(eventEnd) && slotStart.toLocalDate() == eventStartAtZone.toLocalDate()) {
        count += 1
        slotStart = slotStart.plusMinutes(slotDurationMinutes)
    }
    return count.coerceAtLeast(1)
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
