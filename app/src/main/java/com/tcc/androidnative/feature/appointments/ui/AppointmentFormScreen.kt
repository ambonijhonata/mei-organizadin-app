package com.tcc.androidnative.feature.appointments.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.ui.theme.DrawerMenuIconBlue
import com.tcc.androidnative.ui.theme.LoginBrandBlue

@Composable
fun AppointmentFormScreen(
    onCancelClick: () -> Unit,
    viewModel: AppointmentFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppointmentFormContent(
        uiState = uiState,
        onClientSelected = viewModel::selectClient,
        onClientFilterNameChange = viewModel::onClientFilterNameChange,
        onClientSelectorClick = viewModel::onClientSelectorClick,
        onClientOptionRendered = viewModel::onClientOptionRendered,
        onServiceFilterDescriptionChange = viewModel::onServiceFilterDescriptionChange,
        onServiceSelectorClick = viewModel::onServiceSelectorClick,
        onServiceOptionRendered = viewModel::onServiceOptionRendered,
        onStartTimeChange = viewModel::updateStartTime,
        onEndTimeChange = viewModel::updateEndTime,
        onServiceToggled = viewModel::toggleService,
        onSaveClick = viewModel::saveLocalOnly,
        onCancelClick = onCancelClick
    )
}

@Composable
fun AppointmentFormContent(
    uiState: AppointmentFormUiState,
    onClientSelected: (Long) -> Unit,
    onClientFilterNameChange: (String) -> Unit = {},
    onClientSelectorClick: () -> Unit = {},
    onClientOptionRendered: (Int) -> Unit = {},
    onServiceFilterDescriptionChange: (String) -> Unit = {},
    onServiceSelectorClick: () -> Unit = {},
    onServiceOptionRendered: (Int) -> Unit = {},
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onServiceToggled: (Long) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    var clientMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var servicesMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val clientFieldInteractionSource = remember { MutableInteractionSource() }
    val serviceFieldInteractionSource = remember { MutableInteractionSource() }
    val selectedServices = uiState.selectedServiceOptions.values.toList()
    var clientFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var clientListBounds by remember { mutableStateOf<Rect?>(null) }
    var serviceFieldBounds by remember { mutableStateOf<Rect?>(null) }
    var serviceListBounds by remember { mutableStateOf<Rect?>(null) }

    fun openClientMenu() {
        servicesMenuExpanded = false
        clientMenuExpanded = true
        onClientSelectorClick()
    }

    fun toggleClientMenu() {
        if (clientMenuExpanded) {
            clientMenuExpanded = false
        } else {
            openClientMenu()
        }
    }

    fun openServicesMenu() {
        clientMenuExpanded = false
        servicesMenuExpanded = true
        onServiceSelectorClick()
    }

    fun toggleServicesMenu() {
        if (servicesMenuExpanded) {
            servicesMenuExpanded = false
        } else {
            openServicesMenu()
        }
    }

    LaunchedEffect(clientFieldInteractionSource) {
        clientFieldInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                toggleClientMenu()
            }
        }
    }

    LaunchedEffect(serviceFieldInteractionSource) {
        serviceFieldInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) {
                toggleServicesMenu()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(
                clientMenuExpanded,
                servicesMenuExpanded,
                clientFieldBounds,
                clientListBounds,
                serviceFieldBounds,
                serviceListBounds
            ) {
                detectTapGestures { tapOffset ->
                    if (clientMenuExpanded) {
                        val insideClientArea = (clientFieldBounds?.contains(tapOffset) == true) ||
                            (clientListBounds?.contains(tapOffset) == true)
                        if (!insideClientArea) {
                            clientMenuExpanded = false
                        }
                    }
                    if (servicesMenuExpanded) {
                        val insideServiceArea = (serviceFieldBounds?.contains(tapOffset) == true) ||
                            (serviceListBounds?.contains(tapOffset) == true)
                        if (!insideServiceArea) {
                            servicesMenuExpanded = false
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.appointment_form_title),
                style = MaterialTheme.typography.headlineMedium,
                color = DrawerMenuIconBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            uiState.feedbackMessage?.let { message ->
                FeedbackMessageCard(
                    message = message,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appointment-client-field")
                            .onGloballyPositioned { coordinates ->
                                clientFieldBounds = coordinates.boundsInRoot()
                            },
                        value = uiState.clientFilterName,
                        onValueChange = {
                            if (!clientMenuExpanded) {
                                clientMenuExpanded = true
                                servicesMenuExpanded = false
                            }
                            onClientFilterNameChange(it)
                        },
                        readOnly = false,
                        label = { Text(stringResource(R.string.appointment_client_label)) },
                        placeholder = { Text(stringResource(R.string.appointment_client_placeholder)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { toggleClientMenu() }
                            )
                        },
                        enabled = !uiState.isLoadingServices,
                        interactionSource = clientFieldInteractionSource,
                        singleLine = true
                    )
                    if (clientMenuExpanded) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .heightIn(max = 280.dp)
                                .testTag("appointment-client-options")
                                .onGloballyPositioned { coordinates ->
                                    clientListBounds = coordinates.boundsInRoot()
                                },
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                itemsIndexed(
                                    items = uiState.clients,
                                    key = { _, client -> client.id }
                                ) { index, client ->
                                    LaunchedEffect(index, uiState.clients.size) {
                                        onClientOptionRendered(index)
                                    }
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onClientSelected(client.id)
                                                clientMenuExpanded = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        text = client.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

                                if (uiState.isAppendingClients) {
                                    item {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("appointment-start-time"),
                        value = uiState.startTime,
                        onValueChange = { onStartTimeChange(maskTimeInput(it)) },
                        label = { Text(stringResource(R.string.appointment_start_time_label)) },
                        placeholder = { Text("00:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("appointment-end-time"),
                        value = uiState.endTime,
                        onValueChange = { onEndTimeChange(maskTimeInput(it)) },
                        label = { Text(stringResource(R.string.appointment_end_time_label)) },
                        placeholder = { Text("00:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Column {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("appointment-services-field")
                            .onGloballyPositioned { coordinates ->
                                serviceFieldBounds = coordinates.boundsInRoot()
                            },
                        value = uiState.serviceFilterDescription,
                        onValueChange = {
                            if (!servicesMenuExpanded) {
                                servicesMenuExpanded = true
                                clientMenuExpanded = false
                            }
                            onServiceFilterDescriptionChange(it)
                        },
                        readOnly = false,
                        label = { Text(stringResource(R.string.appointment_services_label)) },
                        placeholder = { Text(stringResource(R.string.appointment_services_placeholder)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.clickable { toggleServicesMenu() }
                            )
                        },
                        enabled = !uiState.isLoadingClients,
                        interactionSource = serviceFieldInteractionSource,
                        singleLine = true
                    )
                    if (servicesMenuExpanded) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .heightIn(max = 280.dp)
                                .testTag("appointment-services-options")
                                .onGloballyPositioned { coordinates ->
                                    serviceListBounds = coordinates.boundsInRoot()
                                },
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                itemsIndexed(
                                    items = uiState.services,
                                    key = { _, service -> service.id }
                                ) { index, service ->
                                    LaunchedEffect(index, uiState.services.size) {
                                        onServiceOptionRendered(index)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onServiceToggled(service.id) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = service.id in uiState.selectedServiceIds,
                                            onCheckedChange = null
                                        )
                                        Text(service.description)
                                    }
                                }

                                if (uiState.isAppendingServices) {
                                    item {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    selectedServices.forEach { service ->
                        FilterChip(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("appointment-selected-service-${service.id}"),
                            selected = true,
                            onClick = { onServiceToggled(service.id) },
                            label = { Text(service.description) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Acoes do formulario de agendamento" },
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    modifier = Modifier.testTag("appointment-save"),
                    onClick = onSaveClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginBrandBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.appointment_save_button))
                }
                TextButton(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .testTag("appointment-cancel"),
                    onClick = onCancelClick
                ) {
                    Text(stringResource(R.string.appointment_cancel_button))
                }
            }
        }
    }
}

private fun maskTimeInput(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return when {
        digits.length <= 2 -> digits
        else -> "${digits.take(2)}:${digits.drop(2)}"
    }
}
