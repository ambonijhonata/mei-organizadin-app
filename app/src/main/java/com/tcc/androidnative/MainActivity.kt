package com.tcc.androidnative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tcc.androidnative.navigation.AppNavHost
import com.tcc.androidnative.ui.theme.AndroidNativeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidNativeTheme {
                AppNavHost()
            }
        }
    }
}

