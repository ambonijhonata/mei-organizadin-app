package com.tcc.androidnative

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.tcc.androidnative.core.session.SessionRefreshLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppApplication : Application() {
    @Inject
    lateinit var sessionRefreshLifecycleObserver: SessionRefreshLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(sessionRefreshLifecycleObserver)
    }
}
