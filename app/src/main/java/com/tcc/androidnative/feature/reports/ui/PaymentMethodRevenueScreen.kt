package com.tcc.androidnative.feature.reports.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.R
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.feature.payments.ui.PaymentMethod
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import java.time.LocalDate

@Composable
fun PaymentMethodRevenueScreen(
    viewModel: PaymentMethodRevenueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PaymentMethodRevenueScreenContent(
        uiState = uiState,
        onPeriodSelected = viewModel::onPeriodSelected,
        onEmit = viewModel::emitReport,
        onBackToForm = viewModel::backToForm
    )
}

@Composable
internal fun PaymentMethodRevenueScreenContent(
    uiState: PaymentMethodRevenueUiState,
    onPeriodSelected: (LocalDate, LocalDate) -> Unit,
    onEmit: () -> Unit,
    onBackToForm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.payment_method_revenue_title),
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
            PaymentMethodRevenueScreenStep.FORM -> PaymentMethodRevenueFormStep(
                uiState = uiState,
                onPeriodSelected = onPeriodSelected,
                onEmit = onEmit
            )
            PaymentMethodRevenueScreenStep.REPORT -> PaymentMethodRevenueReportStep(
                uiState = uiState,
                onBackToForm = onBackToForm
            )
        }
    }
}

@Composable
private fun PaymentMethodRevenueFormStep(
    uiState: PaymentMethodRevenueUiState,
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
            ReportPeriodInputField(
                value = uiState.periodInput,
                selectedStartDate = uiState.selectedStartDate,
                selectedEndDate = uiState.selectedEndDate,
                fieldDescription = "Periodo do filtro de faturamento por método de pagamento",
                calendarDescription = "Abrir calendario do periodo do faturamento por método de pagamento",
                pickerTestTag = "payment-method-period-picker",
                confirmButtonTag = "payment-method-period-confirm",
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
                    .semantics { contentDescription = "Emitir relatório de faturamento por método de pagamento" },
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
private fun PaymentMethodRevenueReportStep(
    uiState: PaymentMethodRevenueUiState,
    onBackToForm: () -> Unit
) {
    val report = uiState.report

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
                text = "Relatório de faturamento por método de pagamento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151),
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onBackToForm,
                modifier = Modifier.semantics {
                    contentDescription = "Voltar para formulario de faturamento por método de pagamento"
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
            PaymentMethodRevenueTableHeader()
            HorizontalDivider(color = Color(0xFFE5E7EB))

            if (report == null) {
                Text(
                    text = "Nenhum resultado para o periodo informado.",
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                )
            } else {
                PaymentMethodRevenueRow(
                    periodLabel = "${DateFormats.toUiDate(report.startDate)} - ${DateFormats.toUiDate(report.endDate)}",
                    totalLabel = "-"
                )
                report.entries.forEach { entry ->
                    PaymentMethodRevenueRow(
                        periodLabel = paymentMethodLabel(entry.method),
                        totalLabel = CurrencyFormats.formatForUi(entry.total)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodRevenueTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Periodo",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Total",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF374151),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaymentMethodRevenueRow(
    periodLabel: String,
    totalLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = periodLabel,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = totalLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun paymentMethodLabel(method: PaymentMethod): String {
    val labelRes = when (method) {
        PaymentMethod.DINHEIRO -> R.string.payments_method_dinheiro
        PaymentMethod.PIX -> R.string.payments_method_pix
        PaymentMethod.CREDITO -> R.string.payments_method_credito
        PaymentMethod.DEBITO -> R.string.payments_method_debito
    }
    return stringResource(labelRes)
}
