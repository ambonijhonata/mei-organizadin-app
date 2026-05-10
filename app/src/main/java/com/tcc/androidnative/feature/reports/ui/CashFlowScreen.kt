package com.tcc.androidnative.feature.reports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.tcc.androidnative.R
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.reports.data.CashFlowEntryModel
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import java.time.LocalDate

private const val DETAIL_PERIOD_WEIGHT = 1.1f
private const val DETAIL_SERVICE_WEIGHT = 1.35f
private const val DETAIL_QUANTITY_WEIGHT = 0.45f
private const val DETAIL_TOTAL_WEIGHT = 0.9f
private val CASH_FLOW_COMPACT_BREAKPOINT = 360.dp

private enum class CashFlowLayoutMode {
    DEFAULT,
    COMPACT
}

private data class CashFlowReportLayoutSpec(
    val mode: CashFlowLayoutMode,
    val horizontalPadding: Dp,
    val rowVerticalPadding: Dp,
    val periodWeight: Float,
    val totalWeight: Float,
    val servicesWeight: Float,
    val serviceMaxLines: Int
)

private fun cashFlowReportLayoutSpecFor(availableWidth: Dp): CashFlowReportLayoutSpec {
    val mode = if (availableWidth < CASH_FLOW_COMPACT_BREAKPOINT) {
        CashFlowLayoutMode.COMPACT
    } else {
        CashFlowLayoutMode.DEFAULT
    }

    return when (mode) {
        CashFlowLayoutMode.DEFAULT -> CashFlowReportLayoutSpec(
            mode = mode,
            horizontalPadding = 12.dp,
            rowVerticalPadding = 10.dp,
            periodWeight = 1f,
            totalWeight = 1f,
            servicesWeight = 1f,
            serviceMaxLines = 2
        )

        CashFlowLayoutMode.COMPACT -> CashFlowReportLayoutSpec(
            mode = mode,
            horizontalPadding = 8.dp,
            rowVerticalPadding = 8.dp,
            periodWeight = 0.9f,
            totalWeight = 1.15f,
            servicesWeight = 0.95f,
            serviceMaxLines = 1
        )
    }
}

@Composable
fun CashFlowScreen(
    viewModel: CashFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CashFlowScreenContent(
        uiState = uiState,
        onPeriodSelected = viewModel::onPeriodSelected,
        onEmit = viewModel::emitReport,
        onBackToForm = viewModel::backToForm,
        onOpenDetail = viewModel::openDetail,
        onBackToReport = viewModel::backToReport
    )
}

@Composable
internal fun CashFlowScreenContent(
    uiState: CashFlowUiState,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    onEmit: () -> Unit,
    onBackToForm: () -> Unit,
    onOpenDetail: (CashFlowEntryModel) -> Unit,
    onBackToReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Fluxo de caixa",
            style = MaterialTheme.typography.headlineMedium,
            color = DrawerMenuIconBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        uiState.transientMessage?.let { message ->
            ReportMessageCard(message = message)
            Spacer(modifier = Modifier.height(10.dp))
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val reportLayoutSpec = cashFlowReportLayoutSpecFor(maxWidth)
            val detailLayoutMode = reportLayoutSpec.mode

            when (uiState.step) {
                CashFlowScreenStep.FORM -> CashFlowFormStep(
                    uiState = uiState,
                    onPeriodSelected = onPeriodSelected,
                    onEmit = onEmit
                )
                CashFlowScreenStep.REPORT -> CashFlowReportStep(
                    uiState = uiState,
                    layoutSpec = reportLayoutSpec,
                    onBackToForm = onBackToForm,
                    onOpenDetail = onOpenDetail
                )
                CashFlowScreenStep.DETAIL -> CashFlowDetailStep(
                    selectedDetail = uiState.selectedDetail,
                    layoutMode = detailLayoutMode,
                    onBackToReport = onBackToReport
                )
            }
        }
    }
}

@Composable
private fun CashFlowFormStep(
    uiState: CashFlowUiState,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    onEmit: () -> Unit
) {
    ReportCardContainer {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Emitir relatório",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(12.dp))
            CashFlowPeriodInputField(
                value = uiState.periodInput,
                selectedStartDate = uiState.selectedStartDate,
                selectedEndDate = uiState.selectedEndDate,
                onPeriodSelected = onPeriodSelected,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onEmit,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .semantics { contentDescription = "Emitir relatório de fluxo de caixa" },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginBrandBlue,
                    contentColor = Color.White,
                    disabledContainerColor = LoginBrandBlue.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text(if (uiState.isLoading) "Emitindo..." else "Emitir")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CashFlowPeriodInputField(
    value: String,
    selectedStartDate: LocalDate?,
    selectedEndDate: LocalDate?,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDatePickerVisible by rememberSaveable { mutableStateOf(false) }

    if (isDatePickerVisible) {
        CashFlowPeriodPickerDialog(
            initialSelectedStartDate = selectedStartDate ?: LocalDate.now(),
            initialSelectedEndDate = selectedEndDate,
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
        label = { Text("Periodo (dd/MM/yyyy)") },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(
                onClick = { isDatePickerVisible = true },
                modifier = Modifier.semantics {
                    contentDescription = "Abrir calendario do periodo do fluxo de caixa"
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "Periodo do filtro de fluxo de caixa"
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashFlowPeriodPickerDialog(
    initialSelectedStartDate: LocalDate,
    initialSelectedEndDate: LocalDate?,
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
    val isRangeWithinOneMonth = selectedStartDate != null &&
        selectedEndDate != null &&
        !selectedEndDate.isAfter(selectedStartDate.plusMonths(1))
    val isConfirmEnabled = selectedStartDate != null && selectedEndDate != null && isRangeWithinOneMonth

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
                modifier = Modifier.testTag("cashflow-period-confirm")
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
            showModeToggle = false,
            modifier = Modifier.testTag("cashflow-period-picker")
        )
    }
}

@Composable
private fun CashFlowReportStep(
    uiState: CashFlowUiState,
    layoutSpec: CashFlowReportLayoutSpec,
    onBackToForm: () -> Unit,
    onOpenDetail: (CashFlowEntryModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Relatório de fluxo de caixa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onBackToForm,
                modifier = Modifier.semantics {
                    contentDescription = "Voltar para formulario de fluxo de caixa"
                }
            ) {
                Text("Voltar")
            }
        }

        if (uiState.staleDataWarning) {
            Spacer(modifier = Modifier.height(10.dp))
            ReportInfoBanner(text = stringResource(R.string.feedback_report_stale_data))
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReportCardContainer {
            ReportTableHeader(
                layoutSpec = layoutSpec,
                firstLabel = "Periodo",
                secondLabel = "Total",
                thirdLabel = "Serviços"
            )
            HorizontalDivider(color = Color(0xFFE5E7EB))

            val entries = uiState.report?.entries.orEmpty()
            if (entries.isEmpty()) {
                Text(
                    text = "Nenhum resultado para o periodo informado.",
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = layoutSpec.horizontalPadding,
                                vertical = layoutSpec.rowVerticalPadding
                            ),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = DateFormats.toUiDate(entry.date),
                            modifier = Modifier.weight(layoutSpec.periodWeight),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = CurrencyFormats.formatForUi(entry.total),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(layoutSpec.totalWeight),
                            maxLines = 1,
                            softWrap = false
                        )
                        Column(
                            modifier = Modifier.weight(layoutSpec.servicesWeight),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (entry.services.isEmpty()) {
                                Text(text = "-", color = Color(0xFF6B7280))
                            } else {
                                entry.services.forEach { service ->
                                    Text(
                                        text = service.name,
                                        color = LoginBrandBlue,
                                        textDecoration = TextDecoration.Underline,
                                        maxLines = layoutSpec.serviceMaxLines,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .clickable { onOpenDetail(entry) }
                                            .semantics {
                                                contentDescription = "Abrir detalhamento de ${service.name} em ${DateFormats.toUiDate(entry.date)}"
                                            }
                                    )
                                }
                            }
                        }
                    }
                    if (index < entries.lastIndex) {
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowDetailStep(
    selectedDetail: CashFlowEntryModel?,
    layoutMode: CashFlowLayoutMode,
    onBackToReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Detalhamento de serviços",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onBackToReport,
                modifier = Modifier.semantics {
                    contentDescription = "Voltar para relatório de fluxo de caixa"
                }
            ) {
                Text("Voltar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReportCardContainer {
            if (layoutMode == CashFlowLayoutMode.COMPACT) {
                CompactCashFlowDetailHeader()
            } else {
                ReportTableHeaderWithFourColumns(
                    firstLabel = "Periodo",
                    secondLabel = "Serviço",
                    thirdLabel = "Qtd.",
                    fourthLabel = "Total"
                )
            }
            HorizontalDivider(color = Color(0xFFE5E7EB))

            val detail = selectedDetail
            if (detail == null || detail.services.isEmpty()) {
                Text(
                    text = "Nenhum serviço para detalhar.",
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
            } else {
                detail.services.forEachIndexed { index, service ->
                    if (layoutMode == CashFlowLayoutMode.COMPACT) {
                        CompactCashFlowDetailRow(
                            dateLabel = DateFormats.toShortYearUiDate(detail.date),
                            serviceName = service.name,
                            quantityLabel = service.quantity.toString(),
                            totalLabel = CurrencyFormats.formatForUi(service.total)
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateFormats.toShortYearUiDate(detail.date),
                                modifier = Modifier.weight(DETAIL_PERIOD_WEIGHT),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = service.name,
                                modifier = Modifier.weight(DETAIL_SERVICE_WEIGHT),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = service.quantity.toString(),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(DETAIL_QUANTITY_WEIGHT),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = CurrencyFormats.formatForUi(service.total),
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(DETAIL_TOTAL_WEIGHT),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (index < detail.services.lastIndex) {
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportTableHeader(
    layoutSpec: CashFlowReportLayoutSpec,
    firstLabel: String,
    secondLabel: String,
    thirdLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6))
            .padding(
                horizontal = layoutSpec.horizontalPadding,
                vertical = layoutSpec.rowVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = firstLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(layoutSpec.periodWeight),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = secondLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            textAlign = TextAlign.End,
            modifier = Modifier.weight(layoutSpec.totalWeight),
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = thirdLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(layoutSpec.servicesWeight),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReportTableHeaderWithFourColumns(
    firstLabel: String,
    secondLabel: String,
    thirdLabel: String,
    fourthLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = firstLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(DETAIL_PERIOD_WEIGHT)
        )
        Text(
            text = secondLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(DETAIL_SERVICE_WEIGHT)
        )
        Text(
            text = thirdLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(DETAIL_QUANTITY_WEIGHT)
        )
        Text(
            text = fourthLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(DETAIL_TOTAL_WEIGHT)
        )
    }
}

@Composable
private fun CompactCashFlowDetailHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("cashflow-detail-compact-header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Periodo",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "Total",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Serviço",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Qtd.",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun CompactCashFlowDetailRow(
    dateLabel: String,
    serviceName: String,
    quantityLabel: String,
    totalLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateLabel,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = totalLabel,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = serviceName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Qtd.: $quantityLabel",
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
