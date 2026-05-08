package com.tcc.androidnative.feature.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tcc.androidnative.ui.theme.LoginBrandBlue

@Composable
fun OnboardingStep1Screen(
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit
) {
    OnboardingCardContainer(
        title = "Seu faturamento já está na agenda. A gente só extrai ele de lá.",
        onCloseClick = onCloseClick
    ) {
        Text(
            text = "Conecte-se ao Google Agenda e acompanhe fluxo de caixa e faturamento com base nos seus agendamentos.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Automatico e inteligente",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = "Seus agendamentos viram dados financeiros automaticamente",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Visao completa do negocio",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
        Text(
            text = "Fluxo de caixa e faturamento sempre atualizados",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.height(24.dp))
        OnboardingActions(
            showBack = false,
            onBackClick = {},
            forwardLabel = "",
            onForwardClick = onNextClick
        )
    }
}

@Composable
fun OnboardingStep2Screen(
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    onAddServiceClick: () -> Unit
) {
    OnboardingCardContainer(
        title = "Primeiro, cadastre os serviços que voce oferece",
        onCloseClick = onCloseClick
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = onAddServiceClick,
                shape = CircleShape,
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Cadastrar serviços",
                    tint = LoginBrandBlue
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cadastrar serviços",
                color = Color(0xFF374151),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        OnboardingActions(
            showBack = true,
            onBackClick = onBackClick,
            forwardLabel = "",
            onForwardClick = onNextClick
        )
    }
}

@Composable
fun OnboardingStep4Screen(
    onCloseClick: () -> Unit,
    onBackClick: () -> Unit,
    onConcludeClick: () -> Unit
) {
    OnboardingCardContainer(
        title = "Pronto!",
        onCloseClick = onCloseClick
    ) {
        Text(
            text = "Agora é só fazer os agendamentos no Google Agenda no formato:",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF374151)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "nome do cliente - serviço 1 + serviço 2",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF111827)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF3F4F6),
            tonalElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Exemplo: Joelma - Sobrancelha + buco",
                modifier = Modifier.padding(12.dp),
                color = Color(0xFF4B5563)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Podem ser informados quantos serviços forem necessários",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4B5563)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
                Text(
                    text = " Voltar",
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
            Button(
                onClick = onConcludeClick,
                modifier = Modifier.weight(1.4f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LoginBrandBlue,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Text(
                    text = " Concluir",
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun OnboardingCardContainer(
    title: String,
    onCloseClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFF345BFF), Color(0xFF4E7AF2))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 36.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE5E7EB),
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        IconButton(
                            onClick = onCloseClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar onboarding",
                                tint = Color(0xFF1F2937)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun OnboardingActions(
    showBack: Boolean,
    onBackClick: () -> Unit,
    forwardLabel: String,
    onForwardClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showBack) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
                Text(
                    text = " Voltar",
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        Button(
            onClick = onForwardClick,
            modifier = Modifier.weight(1.4f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LoginBrandBlue,
                contentColor = Color.White
            )
        ) {
            Text(text = forwardLabel)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Proximo onboarding"
            )
        }
    }
}
