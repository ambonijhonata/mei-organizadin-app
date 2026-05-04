package com.tcc.androidnative.core.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
class SessionRefreshLifecycleObserver @Inject constructor(
    private val sessionRefreshCoordinator: SessionRefreshCoordinator
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        scope.launch {
            sessionRefreshCoordinator.refreshIfNeeded(
                force = false,
                trigger = RefreshTrigger.PROACTIVE
            )
        }
    }
}
