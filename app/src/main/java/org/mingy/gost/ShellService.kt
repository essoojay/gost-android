package org.mingy.gost

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.nio.charset.Charset
import java.util.Random


class ShellService : LifecycleService() {
    private val _processThreads = MutableStateFlow(mutableMapOf<GostConfig, ShellThread>())
    val processThreads = _processThreads.asStateFlow()

    private val _logText = MutableStateFlow("")
    val logText: StateFlow<String> = _logText

    fun clearLog() {
        _logText.value = ""
    }

    // Binder given to clients
    private val binder = LocalBinder()

    // Random number generator
    private val mGenerator = Random()

    /** method for clients  */
    val randomNumber: Int
        get() = mGenerator.nextInt(100)

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder(), IBinder {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ShellService = this@ShellService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val gostConfig: ArrayList<GostConfig>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.extras?.getParcelableArrayList(
                    IntentExtraKey.GostConfig, GostConfig::class.java
                )
            } else {
                @Suppress("DEPRECATION") intent?.extras?.getParcelableArrayList(IntentExtraKey.GostConfig)
            }
        if (gostConfig == null) {
            Log.e("gost", "gostConfig is null")
            Toast.makeText(this, "gostConfig is null", Toast.LENGTH_SHORT).show()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ShellServiceAction.START -> {
                for (config in gostConfig) {
                    startGost(config)
                }
                Toast.makeText(this, getString(R.string.service_start_toast), Toast.LENGTH_SHORT)
                    .show()
                startForeground(1, showNotification())
            }

            ShellServiceAction.STOP -> {
                for (config in gostConfig) {
                    stopGost(config)
                }
                startForeground(1, showNotification())
                if (_processThreads.value.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION") stopForeground(true)
                    }
                    stopSelf()
                    Toast.makeText(this, getString(R.string.service_stop_toast), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startGost(config: GostConfig) {
        Log.d("gost", "Start config is $config")
        val dir = Commons.getDir(this)
        val file = config.getFile(this)
        if (!file.exists()) {
            Log.w("gost", "File is not exist, service won't start.")
            Toast.makeText(this, "File is not exist, service won't start.", Toast.LENGTH_SHORT).show()
            return
        }
        if (_processThreads.value.contains(config)) {
            Log.w("gost", "Gost is already running")
            Toast.makeText(this, "Gost is already running", Toast.LENGTH_SHORT).show()
            return
        }
        val ainfo = packageManager.getApplicationInfo(
            packageName, PackageManager.GET_SHARED_LIBRARY_FILES
        )
        val launch = file.readText(Charset.forName("UTF-8"))
        val commandList =
            (listOf("${ainfo.nativeLibraryDir}/${BuildConfig.GostFileName}") + launch.trim().split("\\s+|\\r?\\n+".toRegex()).filter {
                !it.isEmpty()
            }).toList()
        Log.d("gost", "${dir}\n${commandList}")
        try {
            val thread = runCommand(commandList, dir)
            _processThreads.update { it.toMutableMap().apply { put(config, thread) } }
        } catch (e: Exception) {
            Log.e("gost", e.stackTraceToString())
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun stopGost(config: GostConfig) {
        val thread = _processThreads.value.get(config)
//        thread?.interrupt()
        thread?.stopProcess()
        _processThreads.update {
            it.toMutableMap().apply { remove(config) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!_processThreads.value.isEmpty()) {
            _processThreads.value.forEach {
//                it.value.interrupt()
                it.value.stopProcess()
            }
            _processThreads.update { it.clear();it }
        }
    }

    private fun runCommand(command: List<String>, dir: File): ShellThread {
        val process_thread = ShellThread(command, dir) { _logText.value += it + "\n" }
        process_thread.start()
        return process_thread;
    }

    private fun showNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            }
        val notification = NotificationCompat.Builder(this, "shell_bg")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(getString(R.string.gost_notification_title)).setContentText(
                getString(
                    R.string.gost_notification_content, _processThreads.value.size
                )
            )
            //.setTicker("test")
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .build()
        } else {
            return notification.build()
        }
    }
}