package com.deliriousvoid.openvkmatcha.util

import android.util.Log
import com.deliriousvoid.openvkmatcha.BuildConfig

/**
 * Custom logger that suppresses debug logs in release builds.
 */
object MLog {
    private const val DEFAULT_TAG = "Matcha"
    private val IS_DEBUG = BuildConfig.DEBUG

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (IS_DEBUG) {
            Log.d(tag, message)
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Use for logging API responses or other data that might contain sensitive info.
     * In release builds, this will only log the size or a generic message.
     */
    fun data(tag: String = DEFAULT_TAG, label: String, data: String?) {
        if (IS_DEBUG) {
            Log.d(tag, "$label: $data")
        } else {
            val size = data?.length ?: 0
            Log.i(tag, "$label (size: $size bytes)")
        }
    }
}
