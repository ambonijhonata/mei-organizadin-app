package com.tcc.androidnative.navigation

import androidx.lifecycle.ViewModel
import com.tcc.androidnative.feature.onboarding.data.FirstAccessPreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class InitialSetupViewModel @Inject constructor(
    private val firstAccessPreferenceStore: FirstAccessPreferenceStore
) : ViewModel() {
    fun shouldShowOnboarding(): Boolean {
        return !firstAccessPreferenceStore.isFirstAcessCompleted()
    }

    fun completeOnboarding() {
        firstAccessPreferenceStore.setFirstAcessCompleted(completed = true)
    }

    fun hasCompletedOnboarding(): Boolean {
        return firstAccessPreferenceStore.isFirstAcessCompleted()
    }
}
