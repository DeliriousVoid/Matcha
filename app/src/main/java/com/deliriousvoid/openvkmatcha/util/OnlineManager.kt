package com.deliriousvoid.openvkmatcha.util

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.deliriousvoid.openvkmatcha.data.repository.AuthRepository
import kotlinx.coroutines.*

class OnlineManager(
    private val context: Context,
    private val authRepository: AuthRepository
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private var isAppInForeground = false
    private var job: Job? = null
    
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun start() {
        if (isRunning) return
        isRunning = true
        Log.d("OnlineManager", "Starting Online periodic updates")
        resume()
    }

    fun stop() {
        Log.d("OnlineManager", "Stopping Online periodic updates")
        isRunning = false
        pause()
    }

    private fun resume() {
        if (!isRunning || job != null) return
        
        job = scope.launch {
            while (isActive) {
                if (isAppInForeground && !prefs.getBoolean("invisibility", false)) {
                    try {
                        authRepository.setOnline().onSuccess {
                            Log.d("OnlineManager", "Status updated successfully")
                        }.onFailure {
                            Log.e("OnlineManager", "Failed to update status: ${it.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("OnlineManager", "Error updating status", e)
                    }
                } else {
                    Log.d("OnlineManager", "Skipping update: foreground=$isAppInForeground, invisibility=${prefs.getBoolean("invisibility", false)}")
                }
                
                // VK/OpenVK online lasts for ~15 minutes, 
                // but usually apps send it every 5 minutes to be safe.
                delay(5 * 60 * 1000)
            }
        }
    }

    private fun pause() {
        job?.cancel()
        job = null
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d("OnlineManager", "App moved to foreground")
        isAppInForeground = true
        
        // Immediate update on start/resume
        if (isRunning && !prefs.getBoolean("invisibility", false)) {
            scope.launch {
                try {
                    authRepository.setOnline()
                    Log.d("OnlineManager", "Immediate status update successful")
                } catch (e: Exception) {
                    Log.e("OnlineManager", "Immediate update failed", e)
                }
            }
        }
        
        resume()
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d("OnlineManager", "App moved to background")
        isAppInForeground = false
        pause()
    }
}
