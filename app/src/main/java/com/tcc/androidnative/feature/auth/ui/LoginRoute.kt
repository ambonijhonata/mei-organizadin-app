package com.tcc.androidnative.feature.auth.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val TAG = "LoginRoute"

@Composable
fun LoginRoute(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when {
            result.resultCode == Activity.RESULT_CANCELED -> {
                logInfo(
                    "auth_login_google_result canceled resultCode=${result.resultCode} dataPresent=${result.data != null}"
                )
            }
            result.data == null -> {
                logWarn(
                    "auth_login_google_result empty_intent resultCode=${result.resultCode} dataPresent=false"
                )
            }
            else -> {
                logInfo(
                    "auth_login_google_result received resultCode=${result.resultCode} dataPresent=true"
                )
            }
        }
        viewModel.onGoogleSignInResult(result.data)
    }

    LaunchedEffect(viewModel) {
        viewModel.loginSuccessEvents.collect {
            onLoginSuccess()
        }
    }

    LoginScreen(
        uiState = uiState.value,
        onGoogleClick = {
            launcher.launch(viewModel.signInIntent())
        }
    )
}

private fun logInfo(message: String) {
    runCatching { Log.i(TAG, message) }
}

private fun logWarn(message: String) {
    runCatching { Log.w(TAG, message) }
}
