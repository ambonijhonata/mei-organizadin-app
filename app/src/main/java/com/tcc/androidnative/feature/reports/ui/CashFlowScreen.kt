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
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun CashFlowScreen(
    viewModel: CashFlowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                onStartDateChange = viewModel::onStartDateChange,
                onEndDateChange = viewModel::onEndDateChange,
                onEmit = viewModel::emitReport
            )
            CashFlowScreenStep.REPORT -> CashFlowReportStep(
                uiState = uiState,
                onBackToForm = viewModel::backToForm,
                onOpenDetail = viewModel::openDetail
            )
            CashFlowScreenStep.DETAIL -> CashFlowDetailStep(
                selectedDetail = uiState.selectedDetail,
                onBackToReport = viewModel::backToReport
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
                text = "Emitir relatorio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.startDateInput,
                onValueChange = onStartDateChange,
                label = { Text("Data inicial (dd/MM/yyyy)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.endDateInput,
                onValueChange = onEndDateChange,
                label = { Text("Data final (dd/MM/yyyy)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onEmit,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .semantics { contentDescription = "Emitir relatorio de fluxo de caixa" },
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
                text = "Relatorio de fluxo de caixa",
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
                thirdLabel = "Servicos"
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
                text = "Detalhamento de servicos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onBackToReport,
                modifier = Modifier.semantics {
                    contentDescription = "Voltar para relatorio de fluxo de caixa"
                }
            ) {
                Text("Voltar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReportCardContainer {
            ReportTableHeader(
                firstLabel = "Periodo",
                secondLabel = "Servico",
                thirdLabel = "Total"
            )
            HorizontalDivider(color = Color(0xFFE5E7EB))

            val detail = selectedDetail
            if (detail == null || detail.services.isEmpty()) {
                Text(
                    text = "Nenhum servico para detalhar.",
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
                            text = DateFormats.toUiDate(detail.date),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = service.name,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = CurrencyFormats.formatForUi(service.total),
                            modifier = Modifier.weight(1f)
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
