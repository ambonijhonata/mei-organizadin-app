package com.tcc.androidnative.feature.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tcc.androidnative.R
import com.tcc.androidnative.core.ui.feedback.FeedbackMessageCard
import com.tcc.androidnative.ui.theme.LoginBackground
import com.tcc.androidnative.ui.theme.LoginBrandBlue
import com.tcc.androidnative.ui.theme.LoginBrandTitleStyle
import com.tcc.androidnative.ui.theme.LoginButtonLabelStyle
import com.tcc.androidnative.ui.theme.LoginCardBackground
import com.tcc.androidnative.ui.theme.LoginCardBorder
import com.tcc.androidnative.ui.theme.LoginCardTitleStyle
import com.tcc.androidnative.ui.theme.LoginSubtitleGray
import com.tcc.androidnative.ui.theme.LoginSubtitleStyle

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onGoogleClick: () -> Unit
) {
    val googleButtonContentDescription = stringResource(R.string.login_google_button_description)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(72.dp))
        Text(
            text = stringResource(R.string.login_brand_line_one),
            style = LoginBrandTitleStyle,
            color = LoginBrandBlue,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.login_brand_line_two),
            style = LoginBrandTitleStyle,
            color = LoginBrandBlue,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_subtitle),
            style = LoginSubtitleStyle,
            color = LoginSubtitleGray,
            textAlign = TextAlign.Center
        )

        uiState.transientMessage?.let { message ->
            Spacer(modifier = Modifier.height(20.dp))
            FeedbackMessageCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                message = message
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            shape = RoundedCornerShape(12.dp),
            color = LoginCardBackground,
            border = BorderStroke(1.dp, LoginCardBorder),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.login_welcome_title),
                    style = LoginCardTitleStyle,
                    color = LoginSubtitleGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .semantics {
                            contentDescription = googleButtonContentDescription
                        },
                    onClick = onGoogleClick,
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LoginBrandBlue,
                        contentColor = Color.White,
                        disabledContainerColor = LoginBrandBlue.copy(alpha = 0.6f),
                        disabledContentColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isLoading) {
                            stringResource(R.string.login_google_button_loading)
                        } else {
                            stringResource(R.string.login_google_button)
                        },
                        style = LoginButtonLabelStyle
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
fun LoginScreen() {
    LoginScreen(uiState = LoginUiState(), onGoogleClick = {})
}
