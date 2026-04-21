package com.tcc.androidnative.feature.clients.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.core.util.DateFormats
import com.tcc.androidnative.core.util.InputMasks
import com.tcc.androidnative.core.ui.feedback.TransientMessage
import com.tcc.androidnative.R
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    viewModel: ClientsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDatePickerVisible = rememberSaveable { mutableStateOf(false) }
    val allVisibleSelected = uiState.items.isNotEmpty() && uiState.items.all { it.id in uiState.selectedIds }

    LaunchedEffect(uiState.formMode) {
        if (uiState.formMode == null) {
            isDatePickerVisible.value = false
        }
    }

    if (isDatePickerVisible.value) {
        val birthDate = runCatching { DateFormats.parseUiDate(uiState.formState.birthDate) }.getOrNull()
            ?: LocalDate.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateFormats.toUtcMillis(birthDate),
            initialDisplayedMonthMillis = DateFormats.toUtcMillis(birthDate.withDayOfMonth(1))
        )
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis
                            ?.let(DateFormats::fromUtcMillis)
                            ?.let(viewModel::onBirthDatePicked)
                        isDatePickerVisible.value = false
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Clientes",
            style = MaterialTheme.typography.headlineMedium,
            color = DrawerMenuIconBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        uiState.transientMessages.forEach { message ->
            FeedbackMessageCard(
                message = message,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.filterName,
                onValueChange = viewModel::onFilterNameChange,
                label = { Text("Filtrar por nome") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Button(
                modifier = Modifier.semantics { contentDescription = "Filtrar clientes por nome" },
                onClick = viewModel::applyFilter,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginBrandBlue,
                    contentColor = Color.White,
                    disabledContainerColor = LoginBrandBlue.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text("Filtrar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        modifier = Modifier.semantics {
                            contentDescription = "Selecionar todos os clientes visiveis"
                        },
                        checked = allVisibleSelected,
                        onCheckedChange = { checked -> viewModel.toggleHeaderSelection(checked) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Cadastrar cliente" },
                            onClick = viewModel::openCreateForm
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Editar cliente selecionado" },
                            enabled = uiState.selectedIds.size == 1,
                            onClick = viewModel::openEditForm
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null
                            )
                        }
                        IconButton(
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Excluir clientes selecionados" },
                            enabled = uiState.selectedIds.isNotEmpty(),
                            onClick = viewModel::deleteSelection
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(44.dp))
                    Text(
                        text = "Nome",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = "Data de nascimento",
                        modifier = Modifier.width(148.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(uiState.items, key = { _, item -> item.id }) { index, item ->
                        viewModel.onListItemRendered(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(44.dp)) {
                                Checkbox(
                                    checked = item.id in uiState.selectedIds,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleRowSelection(item.id, checked)
                                    }
                                )
                            }
                            Text(text = item.name, modifier = Modifier.weight(1f))
                            Text(
                                text = item.dateOfBirth?.let(DateFormats::toUiDate).orEmpty(),
                                modifier = Modifier.width(148.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                    if (uiState.items.isEmpty() && !uiState.isLoading) {
                        item {
                            Text(
                                text = "Nenhum cliente cadastrado",
                                color = Color(0xFF6B7280),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (uiState.isAppending) {
                        item {
                            Text(
                                text = "Carregando mais clientes...",
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.formMode?.let { mode ->
        ClientFormDialog(
            mode = mode,
            formState = uiState.formState,
            isSaving = uiState.isSubmittingForm,
            onDismiss = {
                isDatePickerVisible.value = false
                viewModel.dismissForm()
            },
            onNameChange = { viewModel.updateForm(name = it) },
            onCpfChange = { viewModel.updateForm(cpf = it) },
            onBirthDateChange = { viewModel.updateForm(birthDate = it) },
            onBirthDateCalendarClick = { isDatePickerVisible.value = true },
            onEmailChange = { viewModel.updateForm(email = it) },
            onPhoneChange = { viewModel.updateForm(phone = it) },
            onSubmit = viewModel::submitForm
        )
    }
}

@Composable
private fun ClientFormDialog(
    mode: ClientFormMode,
    formState: ClientFormState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onCpfChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onBirthDateCalendarClick: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val cpfField = remember { mutableStateOf(TextFieldValue("")) }
    val birthDateField = remember { mutableStateOf(TextFieldValue("")) }
    val phoneField = remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(formState.cpf) {
        if (formState.cpf != cpfField.value.text) {
            cpfField.value = textFieldValueAtEnd(formState.cpf)
        }
    }
    LaunchedEffect(formState.birthDate) {
        if (formState.birthDate != birthDateField.value.text) {
            birthDateField.value = textFieldValueAtEnd(formState.birthDate)
        }
    }
    LaunchedEffect(formState.phone) {
        if (formState.phone != phoneField.value.text) {
            phoneField.value = textFieldValueAtEnd(formState.phone)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (mode == ClientFormMode.CREATE) "Cadastro de cliente" else "Edicao de cliente")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                formState.bannerMessage?.let { message ->
                    FeedbackMessageCard(message = message)
                }
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onNameChange,
                    label = { Text("Nome") },
                    isError = formState.fieldErrors.containsKey("name"),
                    supportingText = {
                        formState.fieldErrors["name"]?.let { Text(resolveFormMessageText(it)) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cpfField.value,
                    onValueChange = { incoming ->
                        cpfField.value = maskedFieldValue(incoming, InputMasks::formatCpfInput)
                        onCpfChange(cpfField.value.text)
                    },
                    label = { Text("CPF") },
                    isError = formState.fieldErrors.containsKey("cpf"),
                    supportingText = {
                        formState.fieldErrors["cpf"]?.let { Text(resolveFormMessageText(it)) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = birthDateField.value,
                    onValueChange = { incoming ->
                        birthDateField.value = maskedFieldValue(incoming, InputMasks::formatBirthDateInput)
                        onBirthDateChange(birthDateField.value.text)
                    },
                    label = { Text("Data de nascimento (dd/MM/yyyy)") },
                    trailingIcon = {
                        IconButton(onClick = onBirthDateCalendarClick) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null
                            )
                        }
                    },
                    isError = formState.fieldErrors.containsKey("birthDate"),
                    supportingText = {
                        formState.fieldErrors["birthDate"]?.let { Text(resolveFormMessageText(it)) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    isError = formState.fieldErrors.containsKey("email"),
                    supportingText = {
                        formState.fieldErrors["email"]?.let { Text(resolveFormMessageText(it)) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phoneField.value,
                    onValueChange = { incoming ->
                        phoneField.value = maskedFieldValue(incoming, InputMasks::formatPhoneInput)
                        onPhoneChange(phoneField.value.text)
                    },
                    label = { Text("Telefone") },
                    isError = formState.fieldErrors.containsKey("phone"),
                    supportingText = {
                        formState.fieldErrors["phone"]?.let { Text(resolveFormMessageText(it)) }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = onSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginBrandBlue,
                    contentColor = Color.White,
                    disabledContainerColor = LoginBrandBlue.copy(alpha = 0.6f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text(if (isSaving) "Salvando..." else "Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun textFieldValueAtEnd(text: String): TextFieldValue {
    return TextFieldValue(text = text, selection = TextRange(text.length))
}

private fun maskedFieldValue(
    incoming: TextFieldValue,
    formatter: (String) -> String
): TextFieldValue {
    val formatted = formatter(incoming.text)
    return TextFieldValue(text = formatted, selection = TextRange(formatted.length))
}

@Composable
private fun resolveFormMessageText(message: TransientMessage): String {
    return if (message.textResId != null) {
        if (message.textArgs.isEmpty()) {
            androidx.compose.ui.res.stringResource(message.textResId)
        } else {
            androidx.compose.ui.res.stringResource(message.textResId, *message.textArgs.toTypedArray())
        }
    } else {
        message.text.orEmpty()
    }
}
