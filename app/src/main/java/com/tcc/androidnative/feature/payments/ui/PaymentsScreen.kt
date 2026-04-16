package com.tcc.androidnative.feature.payments.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import kotlinx.coroutines.flow.collect

@Composable
fun PaymentsScreen(
    viewModel: PaymentsViewModel = hiltViewModel(),
    onSaveSuccess: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.saveEvents.collect { event ->
            when (event) {
                PaymentSaveEvent.Saved -> onSaveSuccess()
            }
        }
    }
    PaymentsContent(
        uiState = uiState,
        onAddPaymentClick = viewModel::onAddPaymentClick,
        onRemovePaymentClick = viewModel::onRemovePaymentClick,
        onPaymentMethodChanged = viewModel::onPaymentMethodChanged,
        onPaymentAmountChanged = viewModel::onPaymentAmountChanged,
        onPaymentValueTotalChanged = viewModel::onPaymentValueTotalChanged,
        onSaveClick = viewModel::savePayments,
        onCancelClick = onCancelClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PaymentsContent(
    uiState: PaymentsUiState,
    onAddPaymentClick: () -> Unit,
    onRemovePaymentClick: (Long) -> Unit,
    onPaymentMethodChanged: (Long, PaymentMethod) -> Unit,
    onPaymentAmountChanged: (Long, String) -> Unit,
    onPaymentValueTotalChanged: (Long, Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val addButtonDescription = stringResource(R.string.payments_add_payment_description)
    val saveButtonDescription = stringResource(R.string.payments_save_button_description)
    val cancelButtonDescription = stringResource(R.string.payments_cancel_button_description)
    val removeButtonDescriptionPrefix = stringResource(R.string.payments_remove_payment_description_prefix)
    val loadingPaymentsText = stringResource(R.string.payments_loading_existing_composition)
    val totalsByMethodLabel = stringResource(R.string.payments_totals_by_method_label)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (!uiState.saveErrorMessage.isNullOrBlank()) {
            FeedbackMessageCard(
                text = uiState.saveErrorMessage,
                tone = MessageTone.ERROR
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.isLoadingPayments) {
            FeedbackMessageCard(
                text = loadingPaymentsText,
                tone = MessageTone.INFO
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!uiState.loadErrorMessage.isNullOrBlank()) {
            FeedbackMessageCard(
                text = uiState.loadErrorMessage,
                tone = MessageTone.ERROR
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.payments_total_service_label))
                Text(
                    text = CurrencyFormats.formatForUi(uiState.totalServiceValue),
                    color = LoginBrandBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        uiState.payments.forEachIndexed { index, entry ->
            val paymentNumber = index + 1
            val totalToggleEnabled = uiState.totalValueOwnerPaymentId == null || uiState.totalValueOwnerPaymentId == entry.id

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.payments_payment_title, paymentNumber))
                        IconButton(
                            onClick = { onRemovePaymentClick(entry.id) },
                            modifier = Modifier.semantics {
                                contentDescription = "$removeButtonDescriptionPrefix $paymentNumber"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null
                            )
                        }
                    }

                    Text(text = stringResource(R.string.payments_method_label))
                    PaymentMethodDropdown(
                        paymentId = entry.id,
                        method = entry.method,
                        onMethodSelected = { method ->
                            onPaymentMethodChanged(entry.id, method)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = entry.isValueTotal,
                            enabled = totalToggleEnabled,
                            onCheckedChange = { checked ->
                                onPaymentValueTotalChanged(entry.id, checked)
                            },
                            modifier = Modifier.testTag("payment_total_checkbox_${entry.id}")
                        )
                        Text(text = stringResource(R.string.payments_value_total_checkbox))
                    }

                    Text(text = stringResource(R.string.payments_value_label))
                    OutlinedTextField(
                        value = entry.amountInput,
                        onValueChange = { value -> onPaymentAmountChanged(entry.id, value) },
                        enabled = !entry.isValueTotal,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_amount_${entry.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = onAddPaymentClick,
            enabled = uiState.canAddPayment,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = addButtonDescription }
                .testTag("add_payment_button")
        ) {
            Text(text = stringResource(R.string.payments_add_payment_button))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stringResource(R.string.payments_total_paid_label))
                Text(text = CurrencyFormats.formatForUi(uiState.totalPaid))
            }
        }

        if (uiState.paymentTypeTotals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(text = totalsByMethodLabel)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.paymentTypeTotals.forEach { total ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = methodToLabel(total.method))
                            Text(text = CurrencyFormats.formatForUi(total.total))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onSaveClick,
                enabled = !uiState.isSaving && !uiState.isLoadingPayments,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginBrandBlue
                ),
                modifier = Modifier.semantics { contentDescription = saveButtonDescription }
            ) {
                Text(
                    text = if (uiState.isSaving) {
                        stringResource(R.string.payments_save_button_saving)
                    } else {
                        stringResource(R.string.payments_save_button)
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.semantics { contentDescription = cancelButtonDescription }
            ) {
                Text(stringResource(R.string.payments_cancel_button))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodDropdown(
    paymentId: Long,
    method: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit
) {
    var expanded by remember(paymentId) { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = methodToLabel(method),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("payment_method_$paymentId")
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            PaymentMethod.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(methodToLabel(option)) },
                    onClick = {
                        onMethodSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun methodToLabel(method: PaymentMethod): String {
    val labelRes = when (method) {
        PaymentMethod.DINHEIRO -> R.string.payments_method_dinheiro
        PaymentMethod.PIX -> R.string.payments_method_pix
        PaymentMethod.CREDITO -> R.string.payments_method_credito
        PaymentMethod.DEBITO -> R.string.payments_method_debito
    }
    return stringResource(labelRes)
}
