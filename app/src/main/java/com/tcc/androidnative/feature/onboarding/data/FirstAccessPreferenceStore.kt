package com.tcc.androidnative.feature.onboarding.data

interface FirstAccessPreferenceStore {
    fun isFirstAcessCompleted(): Boolean
    fun setFirstAcessCompleted(completed: Boolean)
}

