package com.deliriousvoid.openvkmatcha.util

import android.content.Context
import android.content.SharedPreferences
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            prefs.edit()
                .putBoolean(KEY_CRASHED, true)
                .putString(KEY_STACK_TRACE, stackTrace)
                .commit()

            MLog.e("CrashHandler", "Crash detected and saved: $stackTrace")
        } catch (e: Exception) {
            MLog.e("CrashHandler", "Error saving crash log", e)
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        private const val PREFS_NAME = "crash_prefs"
        private const val KEY_CRASHED = "crashed"
        private const val KEY_STACK_TRACE = "stack_trace"

        fun hasCrashed(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CRASHED, false)
        }

        fun getCrashLog(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_STACK_TRACE, null)
        }

        fun clearCrashState(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CRASHED, false)
                .remove(KEY_STACK_TRACE)
                .apply()
        }

        fun init(context: Context) {
            if (Thread.getDefaultUncaughtExceptionHandler() !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context))
            }
        }
    }
}
