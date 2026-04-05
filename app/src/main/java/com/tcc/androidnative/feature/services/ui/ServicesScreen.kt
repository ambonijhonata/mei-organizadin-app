package com.tcc.androidnative.feature.services.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.core.ui.feedback.MessageTone
import com.tcc.androidnative.core.util.CurrencyFormats
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.LoginBrandBlue

@Composable
fun ServicesScreen(
    viewModel: ServicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allVisibleSelected = uiState.items.isNotEmpty() && uiState.items.all { it.id in uiState.selectedIds }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Servicos",
            style = MaterialTheme.typography.headlineMedium,
            color = DrawerMenuIconBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        uiState.transientMessage?.let { message ->
            val color = when (message.tone) {
                MessageTone.SUCCESS -> Color(0xFF1B5E20)
                MessageTone.WARNING -> Color(0xFFE65100)
                MessageTone.ERROR -> Color(0xFFC62828)
                MessageTone.INFO -> MaterialTheme.colorScheme.primary
            }
            Text(
                text = message.text,
                color = color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.filterDescription,
                onValueChange = viewModel::onFilterDescriptionChange,
                label = { Text("Filtrar por descricao") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            Button(
                modifier = Modifier.semantics { contentDescription = "Filtrar servicos por descricao" },
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
                            contentDescription = "Selecionar todos os servicos visiveis"
                        },
                        checked = allVisibleSelected,
                        onCheckedChange = { checked -> viewModel.toggleHeaderSelection(checked) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Cadastrar servico" },
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
                                .semantics { contentDescription = "Editar servico selecionado" },
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
                                .semantics { contentDescription = "Excluir servicos selecionados" },
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
                        text = "Descricao",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
                    )
                    Text(
                        text = "Valor",
                        modifier = Modifier.width(120.dp),
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
                            Text(text = item.description, modifier = Modifier.weight(1f))
                            Text(
                                text = CurrencyFormats.formatForUi(item.value),
                                modifier = Modifier.width(120.dp)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF3F4F6))
                    }
                    if (uiState.items.isEmpty() && !uiState.isLoading) {
                        item {
                            Text(
                                text = "Nenhum servico cadastrado",
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
                                text = "Carregando mais servicos...",
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    uiState.formMode?.let { mode ->
        ServiceFormDialog(
            mode = mode,
            formState = uiState.formState,
            isSaving = uiState.isSubmittingForm,
            onDismiss = viewModel::dismissForm,
            onDescriptionChange = { viewModel.updateForm(description = it) },
            onValueChange = { viewModel.updateForm(valueInput = it) },
            onSubmit = viewModel::submitForm
        )
    }
}

@Composable
private fun ServiceFormDialog(
    mode: ServiceFormMode,
    formState: ServiceFormState,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (mode == ServiceFormMode.CREATE) "Cadastro de servico" else "Edicao de servico")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Descricao") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.valueInput,
                    onValueChange = onValueChange,
                    label = { Text("Valor") },
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
