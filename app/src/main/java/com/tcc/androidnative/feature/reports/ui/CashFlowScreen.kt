package com.tcc.androidnative.feature.reports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun CashFlowScreen(
    viewModel: CashFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CashFlowScreenContent(
        uiState = uiState,
        onStartDateChange = viewModel::onStartDateChange,
        onEndDateChange = viewModel::onEndDateChange,
        onEmit = viewModel::emitReport,
        onBackToForm = viewModel::backToForm,
        onOpenDetail = viewModel::openDetail,
        onBackToReport = viewModel::backToReport
    )
}

@Composable
internal fun CashFlowScreenContent(
    uiState: CashFlowUiState,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
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

        when (uiState.step) {
            CashFlowScreenStep.FORM -> CashFlowFormStep(
                uiState = uiState,
                onStartDateChange = onStartDateChange,
                onEndDateChange = onEndDateChange,
                onEmit = onEmit
            )
            CashFlowScreenStep.REPORT -> CashFlowReportStep(
                uiState = uiState,
                onBackToForm = onBackToForm,
                onOpenDetail = onOpenDetail
            )
            CashFlowScreenStep.DETAIL -> CashFlowDetailStep(
                selectedDetail = uiState.selectedDetail,
                onBackToReport = onBackToReport
            )
        }
    }
}

@Composable
private fun CashFlowFormStep(
    uiState: CashFlowUiState,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit,
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
            ReportDateInputField(
                value = uiState.startDateInput,
                label = "Data inicial (dd/MM/yyyy)",
                fieldDescription = "Data inicial do filtro de fluxo de caixa",
                calendarDescription = "Abrir calendario da data inicial do fluxo de caixa",
                onValueChange = onStartDateChange,
                onDateSelected = { date: LocalDate -> onStartDateChange(DateFormats.toUiDate(date)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            ReportDateInputField(
                value = uiState.endDateInput,
                label = "Data final (dd/MM/yyyy)",
                fieldDescription = "Data final do filtro de fluxo de caixa",
                calendarDescription = "Abrir calendario da data final do fluxo de caixa",
                onValueChange = onEndDateChange,
                onDateSelected = { date: LocalDate -> onEndDateChange(DateFormats.toUiDate(date)) },
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

@Composable
private fun CashFlowReportStep(
    uiState: CashFlowUiState,
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = DateFormats.toUiDate(entry.date),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = CurrencyFormats.formatForUi(entry.total),
                            modifier = Modifier.weight(1f)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
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
            ReportTableHeaderWithFourColumns(
                firstLabel = "Periodo",
                secondLabel = "Serviço",
                thirdLabel = "Qtd.",
                fourthLabel = "Total"
            )
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateFormats.toShortYearUiDate(detail.date),
                            modifier = Modifier.weight(DETAIL_PERIOD_WEIGHT)
                        )
                        Text(
                            text = service.name,
                            modifier = Modifier.weight(DETAIL_SERVICE_WEIGHT)
                        )
                        Text(
                            text = service.quantity.toString(),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(DETAIL_QUANTITY_WEIGHT)
                        )
                        Text(
                            text = CurrencyFormats.formatForUi(service.total),
                            modifier = Modifier.weight(DETAIL_TOTAL_WEIGHT)
                        )
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
    firstLabel: String,
    secondLabel: String,
    thirdLabel: String
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
            modifier = Modifier.weight(1f)
        )
        Text(
            text = secondLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = thirdLabel,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(1f)
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
