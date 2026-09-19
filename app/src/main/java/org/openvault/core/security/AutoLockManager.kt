package org.openvault.core.security

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.openvault.core.model.AutoLockTimeout

object AutoLockManager : Application.ActivityLifecycleCallbacks {

    private var autoLockTimeout: AutoLockTimeout = AutoLockTimeout.ONE_MINUTE
    private var lockJob: Job? = null
    private var backgroundTimestamp: Long = 0L
    private var startedActivities = 0

    fun initialize(application: Application, timeout: AutoLockTimeout) {
        this.autoLockTimeout = timeout
        application.registerActivityLifecycleCallbacks(this)
    }

    fun updateTimeout(timeout: AutoLockTimeout) {
        this.autoLockTimeout = timeout
    }

    override fun onActivityStarted(activity: Activity) {
        startedActivities++
        if (startedActivities == 1) {
            // App entered foreground
            if (autoLockTimeout.seconds > 0 && backgroundTimestamp > 0) {
                val elapsedSec = (System.currentTimeMillis() - backgroundTimestamp) / 1000L
                if (elapsedSec >= autoLockTimeout.seconds) {
                    VaultSession.lock()
                } else {
                    lockJob?.cancel()
                }
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities--
        if (startedActivities <= 0) {
            startedActivities = 0
            backgroundTimestamp = System.currentTimeMillis()

            if (!VaultSession.isUnlocked) return

            if (autoLockTimeout == AutoLockTimeout.IMMEDIATE) {
                VaultSession.lock()
            } else if (autoLockTimeout.seconds > 0) {
                lockJob?.cancel()
                lockJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(autoLockTimeout.seconds * 1000L)
                    if (VaultSession.isUnlocked) {
                        VaultSession.lock()
                    }
                }
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
