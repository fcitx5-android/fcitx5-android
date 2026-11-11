package org.fcitx.fcitx5.android.clipboardsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PersistableBundle
import android.os.PowerManager
import android.os.SystemClock
import android.util.Base64
import android.util.Log
import org.fcitx.fcitx5.android.BuildConfig
import org.fcitx.fcitx5.android.R
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.fcitx.fcitx5.android.common.ClipboardMetadata
import org.fcitx.fcitx5.android.common.FcitxPluginService
import org.fcitx.fcitx5.android.common.PluginMessage
import org.fcitx.fcitx5.android.common.ipc.FcitxRemoteConnection
import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer
import org.fcitx.fcitx5.android.common.ipc.bindFcitxRemoteService
import org.fcitx.fcitx5.android.clipboardsync.ui.ClipboardSyncSettingsActivity
import org.fcitx.fcitx5.android.clipboardsync.network.ClipCascadeClient
import org.fcitx.fcitx5.android.clipboardsync.network.ClipCascadeClipboardData
import org.fcitx.fcitx5.android.clipboardsync.network.ClipboardData
import org.fcitx.fcitx5.android.clipboardsync.network.OneClipEventClient
import org.fcitx.fcitx5.android.clipboardsync.network.SyncClient
import org.fcitx.fcitx5.android.clipboardsync.network.SyncClient.ServerBackend
import org.fcitx.fcitx5.android.clipboardsync.service.QuickSyncTileService
import org.fcitx.fcitx5.android.clipboardsync.ui.ClipboardCaptureActivity
import org.fcitx.fcitx5.android.clipboardsync.ui.StoragePathUtils
import java.io.IOException
import java.util.Locale

class MainService : FcitxPluginService() {

    companion object {
        private const val TAG = "FcitxClipboardSync"
        private const val PREF_QUICK_SYNC = "quick_sync"
        private const val DEFAULT_QUICK_SYNC_ENABLED = false
        private const val PREF_QUICK_SYNC_UNREACHABLE = "quick_sync_unreachable"
        private const val PREF_IME_SYNC_ACTIVE = "ime_sync_active"
        private const val PREF_SYNC_INTERVAL = "sync_interval"
        private const val PREF_USERNAME = "username"
        private const val PREF_PASSWORD = "password"
        private const val SERVER_PROFILE_TYPE_KEY = "server_profile_type"
        private const val SERVER_ADDRESS_KEY = "server_address"
        private const val SERVER_ADDRESS_SYNC_CLIPBOARD_KEY = "server_address_syncclipboard"
        private const val SERVER_ADDRESS_ONE_CLIP_KEY = "server_address_oneclip"
        private const val SERVER_ADDRESS_CLIP_CASCADE_KEY = "server_address_clipcascade"
        private const val SERVER_ADDRESS_CUSTOM_KEY = "server_address_custom"
        private const val PROFILE_SYNC_CLIPBOARD = "syncclipboard"
        private const val PROFILE_ONE_CLIP = "oneclip"
        private const val PROFILE_CLIP_CASCADE = "clipcascade"
        private const val PROFILE_CUSTOM = "custom"
        private const val DEFAULT_SYNC_CLIPBOARD_URL = "http://192.168.10.45:5033"
        private const val DEFAULT_ONE_CLIP_URL = "http://192.168.10.45:8899"
        private const val DEFAULT_CLIP_CASCADE_URL = "http://192.168.10.45:8080"
        private val CONNECTIVITY_RETRY_DELAYS_MS = longArrayOf(3_000L, 10_000L, 30_000L)
        private const val EVENT_BACKEND_HEALTH_CHECK_MS = 30_000L
        private const val EVENT_BACKEND_FALLBACK_PULL_MS = 60_000L
        private const val EVENT_BACKEND_STALE_RECONNECT_MS = 120_000L
        private const val SCREEN_OFF_POLL_INTERVAL_SECONDS = 15L
        private const val POWER_SAVE_POLL_INTERVAL_SECONDS = 30L
        private const val AGGRESSIVE_POLL_INTERVAL_SECONDS = 60L
        private const val SCREEN_OFF_HEALTH_CHECK_MS = 60_000L
        private const val POWER_SAVE_HEALTH_CHECK_MS = 120_000L
        private const val AGGRESSIVE_HEALTH_CHECK_MS = 180_000L
        private const val SCREEN_OFF_FALLBACK_PULL_MS = 180_000L
        private const val POWER_SAVE_FALLBACK_PULL_MS = 240_000L
        private const val AGGRESSIVE_FALLBACK_PULL_MS = 300_000L
        private const val SCREEN_OFF_STALE_RECONNECT_MS = 300_000L
        private const val POWER_SAVE_STALE_RECONNECT_MS = 420_000L
        private const val AGGRESSIVE_STALE_RECONNECT_MS = 600_000L
        private const val NETWORK_RECONNECT_DEBOUNCE_MS = 2_000L
        private const val NOTIFICATION_CHANNEL_ID = "clipboard-sync-keepalive"
        private const val NOTIFICATION_ID = 1302
        private const val ACTION_START_SYNC = "org.fcitx.fcitx5.android.clipboardsync.action.START"
        private const val ACTION_RECONNECT_SYNC = "org.fcitx.fcitx5.android.clipboardsync.action.RECONNECT"
        private const val ACTION_PAUSE_SYNC = "org.fcitx.fcitx5.android.clipboardsync.action.PAUSE"
        private const val ACTION_INGEST_CAPTURED_CLIPBOARD = "org.fcitx.fcitx5.android.clipboardsync.action.INGEST_CAPTURED_CLIPBOARD"
        private const val ACTION_SUPPRESS_REMOTE_CLIPBOARD =
            "org.fcitx.fcitx5.android.clipboardsync.action.SUPPRESS_REMOTE_CLIPBOARD"
        private const val EXTRA_START_REASON = "start_reason"
        private const val EXTRA_FORCE_ENABLE_SYNC = "force_enable_sync"
        private const val EXTRA_CAPTURED_CLIPBOARD_CONTENT = "captured_clipboard_content"
        private const val EXTRA_SUPPRESSED_REMOTE_ITEMS = "suppressed_remote_items"
        private const val PREF_PENDING_UPLOADS = "pending_uploads"
        private const val PREF_REMOTE_REVISIONS = "remote_revisions"
        private const val PREF_LAST_SYNCED_CONTENT = "last_synced_content"
        private const val PREF_SUPPRESSED_REMOTE_ITEMS = "suppressed_remote_items"
        private const val MAX_PENDING_UPLOADS = 50
        private const val MAX_SUPPRESSED_REMOTE_ITEMS = 256

        fun shouldAutoStart(context: Context): Boolean {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getBoolean(PREF_QUICK_SYNC, DEFAULT_QUICK_SYNC_ENABLED)
        }

        fun startSyncService(
            context: Context,
            reason: String,
            forceEnableSync: Boolean = false,
            imeSyncActive: Boolean = false
        ) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (!forceEnableSync && !prefs.getBoolean(PREF_QUICK_SYNC, DEFAULT_QUICK_SYNC_ENABLED)) {
                return
            }
            if (imeSyncActive) {
                prefs.edit().putBoolean(PREF_IME_SYNC_ACTIVE, true).apply()
            }
            val intent = Intent(context, MainService::class.java).apply {
                action = ACTION_START_SYNC
                putExtra(EXTRA_START_REASON, reason)
                putExtra(EXTRA_FORCE_ENABLE_SYNC, forceEnableSync)
            }
            context.startService(intent)
        }

        fun stopSyncService(context: Context) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IME_SYNC_ACTIVE, false)
                .apply()
            runCatching {
                context.stopService(Intent(context, MainService::class.java))
            }
        }

        fun submitCapturedClipboard(context: Context, content: String, reason: String = "manual-capture") {
            if (content.isBlank()) return
            val intent = Intent(context, MainService::class.java).apply {
                action = ACTION_INGEST_CAPTURED_CLIPBOARD
                putExtra(EXTRA_START_REASON, reason)
                putExtra(EXTRA_CAPTURED_CLIPBOARD_CONTENT, content)
            }
            context.startService(intent)
        }

        fun suppressRemoteClipboardContents(context: Context, contents: Collection<String>, reason: String = "suppress-remote") {
            if (contents.isEmpty()) return
            val intent = Intent(context, MainService::class.java).apply {
                action = ACTION_SUPPRESS_REMOTE_CLIPBOARD
                putExtra(EXTRA_START_REASON, reason)
                putStringArrayListExtra(EXTRA_SUPPRESSED_REMOTE_ITEMS, ArrayList(contents.distinct()))
            }
            context.startService(intent)
        }
    }

    override val stopOnUnbind: Boolean = false
    override val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                PluginMessage.WHAT_LOCAL_CLIPBOARD_UPDATED -> {
                    val content = msg.data?.getString(PluginMessage.KEY_CLIPBOARD_TEXT).orEmpty()
                    handleLocalClipboardUpdate(content, "fcitx-sync-message")
                }

                PluginMessage.WHAT_UPLOAD_CLIPBOARD_REQUEST -> {
                    val content = msg.data?.getString(PluginMessage.KEY_CLIPBOARD_TEXT).orEmpty()
                    forceUploadClipboard(content, "fcitx-upload-request")
                }

                else -> super.handleMessage(msg)
            }
        }
    }

    private lateinit var prefs: SharedPreferences
    private var connection: FcitxRemoteConnection? = null
    private var syncJob: Job? = null
    private var healthMonitorJob: Job? = null
    private var networkReconnectJob: Job? = null
    private var scope = createScope()
    private var transformerRegistered = false
    private var serviceRunning = false
    private var selfStarted = false
    private var networkCallbackRegistered = false
    private var foregroundActive = false
    private var screenStateReceiverRegistered = false
    private val clipboardManager by lazy {
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val powerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    private var clipboardListenerRegistered = false
    private var prefsListenerRegistered = false
    private var clipCascadeClient: ClipCascadeClient? = null
    private var oneClipClient: OneClipEventClient? = null
    private var lastNetworkAvailableAt = 0L
    private var connectionSessionId = 0
    private var activeEndpointIdentity: String? = null

    // Cache to avoid circular updates (Pull -> Local -> Push -> Loop)
    private var lastLocalContent: String? = null
    private var lastRemoteContent: String? = null
    private var lastRemoteRevision: String? = null
    private var lastUploadedContent: String? = null
    private var lastSuccessfulRemoteSyncAt = 0L
    private var lastBackendActivityAt = 0L
    private var lastClipboardReadFailureLoggedAt = 0L
    private val remoteFetchMutex = Mutex()
    private val pendingUploadMutex = Mutex()
    private val pendingUploadDrainMutex = Mutex()
    private val ignoredRemoteClipboardContents = linkedSetOf<String>()
    private val pendingUploads = mutableListOf<PendingUploadEntry>()
    private val storedRemoteRevisions = mutableMapOf<String, String>()
    private val suppressedRemoteClipboardContents = linkedSetOf<String>()
    // Track imported profile IDs to avoid duplicate imports within a sync cycle
    private val importedProfileIdsInCurrentSync = mutableSetOf<String>()
    private val stateJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val now = SystemClock.elapsedRealtime()
            lastNetworkAvailableAt = now
            scheduleReconnect("network-available")
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                val now = SystemClock.elapsedRealtime()
                if (now - lastNetworkAvailableAt >= NETWORK_RECONNECT_DEBOUNCE_MS) {
                    lastNetworkAvailableAt = now
                    scheduleReconnect("network-capabilities")
                }
            }
        }
    }

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        handleSystemClipboardChanged()
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.d(TAG, "[Power] Screen turned off, stop sync loops to reduce background power")
                    stopPeriodicSync()
                    stopHealthMonitor()
                }
                Intent.ACTION_SCREEN_ON -> scheduleReconnect("screen-on")
                Intent.ACTION_USER_PRESENT -> scheduleReconnect("user-present")
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> scheduleReconnect("power-save-mode")
            }
        }
    }

    private val transformer = object : IClipboardEntryTransformer.Stub() {
        override fun getPriority(): Int = 100

        override fun transform(clipboardText: String): String {
            // Clipboard filters should run first so sync observes the sanitized text.
            // This is called when user copies text locally
            if (clipboardText == lastRemoteContent) {
                // If this change matches what we just pulled, ignore it (don't push back)
                return clipboardText
            }

            handleLocalClipboardUpdate(clipboardText, "transformer")
            return clipboardText
        }

        override fun getDescription(): String = "SyncClipboard"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MainService onCreate")
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        loadPersistentSyncState()
        lastUploadedContent = prefs.getString(PREF_LAST_SYNCED_CONTENT, null)
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureScope()
        selfStarted = true
        handleActionIntent(intent)
        start()
        return START_NOT_STICKY
    }

    override fun start() {
        ensureScope()
        if (serviceRunning) {
            updateForegroundState()
            refreshSyncRuntime()
            ensureRemoteBinding()
            return
        }
        Log.d(TAG, "MainService start")
        serviceRunning = true
        ensureSelfStarted()
        updateForegroundState()
        registerClipboardListenerIfNeeded()
        registerPrefsListenerIfNeeded()
        registerNetworkCallbackIfNeeded()
        registerScreenStateReceiverIfNeeded()
        refreshSyncRuntime()
        ensureRemoteBinding(forceRebind = true)
        handleSystemClipboardChanged()
    }

    override fun stop() {
        if (!serviceRunning) return
        Log.d(TAG, "MainService stop")
        serviceRunning = false
        unregisterClipboardListenerIfNeeded()
        unregisterPrefsListenerIfNeeded()
        unregisterNetworkCallbackIfNeeded()
        unregisterScreenStateReceiverIfNeeded()
        stopPeriodicSync()
        stopHealthMonitor()
        stopForegroundState()
        connectionSessionId += 1
        val activeConnection = connection
        connection = null
        runCatching {
            if (transformerRegistered) {
                activeConnection?.remoteService?.unregisterClipboardEntryTransformer(transformer)
            }
        }
        transformerRegistered = false
        runCatching {
            if (activeConnection != null) {
                unbindService(activeConnection)
            }
        }
        scope.coroutineContext.cancelChildren()
    }

    override fun onDestroy() {
        unregisterClipboardListenerIfNeeded()
        unregisterPrefsListenerIfNeeded()
        unregisterNetworkCallbackIfNeeded()
        unregisterScreenStateReceiverIfNeeded()
        stopPeriodicSync()
        stopHealthMonitor()
        stopForegroundState()
        connectionSessionId += 1
        connection = null
        scope.cancel()
        serviceRunning = false
        selfStarted = false
        super.onDestroy()
    }

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == SERVER_ADDRESS_KEY || key == PREF_USERNAME || key == PREF_PASSWORD || key == SERVER_PROFILE_TYPE_KEY) {
            Log.d(TAG, "Sync credential/config changed: $key, resetting sync cache")
            resetRemoteCache()
            resetFailureState()
            refreshSyncRuntime()
        } else if (
            key == SyncFilterPrefs.PREF_FILTER_BLOCKED_EXTENSIONS ||
            key == SyncFilterPrefs.PREF_FILTER_MAX_FILE_SIZE ||
            key == SyncFilterPrefs.PREF_FILTER_MAX_FILE_SIZE_UNIT ||
            key == SyncFilterPrefs.PREF_FILTER_MIN_TEXT_CHARS ||
            key == SyncFilterPrefs.PREF_FILTER_MAX_TEXT_CHARS
        ) {
            Log.d(TAG, "Receive filter changed: $key")
        } else if (key == PREF_QUICK_SYNC || key == PREF_SYNC_INTERVAL) {
            Log.d(TAG, "Preference changed: $key, restarting sync")
            updateForegroundState()
            refreshSyncRuntime()
            if (key == PREF_QUICK_SYNC) {
                QuickSyncTileService.requestTileRefresh(this)
            }
        }
    }

    private fun handleLocalClipboardUpdate(content: String, origin: String) {
        if (content.isBlank()) return
        val normalizedContent = OutgoingClipboardFilter.transform(this, connection?.remoteService, content)
        if (consumeIgnoredRemoteClipboardContent(normalizedContent)) {
            return
        }
        if (normalizedContent == lastRemoteContent || normalizedContent == lastUploadedContent) {
            return
        }
        if (normalizedContent != lastLocalContent) {
            lastLocalContent = normalizedContent
        }
        scope.launch {
            val queued = enqueuePendingUpload(normalizedContent)
            if (!queued) {
                return@launch
            }
            Log.d(TAG, "[Push] Detected local change from $origin, queued for upload")
            flushPendingUploads("local-change:$origin")
        }
    }

    private fun refreshSyncRuntime() {
        if (!shouldRunSyncLoops()) {
            stopPeriodicSync()
            stopHealthMonitor()
            return
        }
        ensureSelfStarted()
        startPeriodicSync()
        startHealthMonitor()
    }

    private fun startHealthMonitor() {
        if (!shouldRunSyncLoops()) return
        if (healthMonitorJob?.isActive == true) return
        healthMonitorJob = scope.launch {
            while (isActive) {
                if (!shouldRunSyncLoops()) {
                    Log.d(TAG, "[Health] Sync loops are disabled, stop health monitor")
                    break
                }
                delay(currentHealthCheckDelayMs())
                if (!shouldRunSyncLoops()) {
                    Log.d(TAG, "[Health] Sync loops are disabled after delay, stop health monitor")
                    break
                }
                val endpoint = currentEndpoint()
                val backend = endpoint.backend
                if (backend == ServerBackend.SYNCCLIPBOARD) {
                    if (syncJob?.isActive != true) {
                        Log.w(TAG, "[Health] Polling loop is inactive, restarting")
                        startPeriodicSync()
                    }
                    continue
                }

                val connected = when (backend) {
                    ServerBackend.CLIPCASCADE -> clipCascadeClient?.isConnected() == true
                    ServerBackend.ONECLIP -> oneClipClient?.isConnected() == true
                    ServerBackend.SYNCCLIPBOARD -> true
                }
                if (!connected || syncJob?.isActive != true) {
                    Log.w(TAG, "[Health] Event stream is disconnected, restarting $backend")
                    startPeriodicSync()
                    continue
                }

                val now = SystemClock.elapsedRealtime()
                if (now - lastSuccessfulRemoteSyncAt >= currentFallbackPullDelayMs()) {
                    try {
                        checkRemoteClipboard()
                        flushPendingUploads("health-fallback")
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.e(TAG, "[Health] Fallback pull failed for ${endpoint.address}", error)
                        handleConnectivityFailure(endpoint, error)
                    }
                }

                if (now - lastBackendActivityAt >= currentStaleReconnectDelayMs()) {
                    Log.w(TAG, "[Health] Backend stream appears stale, forcing reconnect for $backend")
                    startPeriodicSync()
                }
            }
        }
    }

    private fun stopHealthMonitor() {
        healthMonitorJob?.cancel()
        healthMonitorJob = null
    }

    private fun startPeriodicSync() {
        stopPeriodicSync()

        if (!shouldRunSyncLoops()) {
            Log.d(TAG, "[Pull] Sync loops paused by runtime state (quick-sync off or screen not interactive)")
            return
        }

        when (currentBackend()) {
            ServerBackend.CLIPCASCADE -> {
                Log.d(TAG, "[ClipCascade] Starting persistent websocket sync")
                startClipCascadeSync()
                return
            }

            ServerBackend.ONECLIP -> {
                Log.d(TAG, "[OneClip] Starting persistent SSE sync")
                startOneClipSync()
                return
            }

            ServerBackend.SYNCCLIPBOARD -> Unit
        }

        Log.d(TAG, "[Pull] Starting periodic sync")
        syncJob = scope.launch {
            while (isActive) {
                if (!shouldRunSyncLoops()) {
                    Log.d(TAG, "[Pull] Sync loops are disabled, stop polling loop")
                    break
                }
                val endpoint = currentEndpoint()
                try {
                    val safeInterval = runCatching {
                        prefs.getInt(PREF_SYNC_INTERVAL, 3).toLong()
                    }.getOrElse {
                        prefs.getString(PREF_SYNC_INTERVAL, "3")?.toLongOrNull() ?: 3L
                    }.coerceIn(1, 60)

                    checkRemoteClipboard()
                    resetFailureState()
                    flushPendingUploads("poll-loop")

                    delay(currentPollingIntervalSeconds(safeInterval) * 1000L)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (handleConnectivityFailure(endpoint, e)) {
                        continue
                    }
                    Log.e(TAG, "[Pull] Loop error", e)
                    delay(5000)
                }
            }
        }
    }

    private fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
        networkReconnectJob?.cancel()
        networkReconnectJob = null
        disconnectClipCascadeClient()
        disconnectOneClipClient()
    }

    private fun startClipCascadeSync() {
        disconnectClipCascadeClient()
        syncJob = scope.launch {
            while (isActive) {
                if (!shouldRunSyncLoops()) {
                    Log.d(TAG, "[ClipCascade] Sync loops are disabled, stop websocket loop")
                    break
                }
                val endpoint = currentEndpoint()
                if (endpoint.address.isBlank()) {
                    Log.d(TAG, "[ClipCascade] Server address is blank, skipping websocket sync")
                    return@launch
                }

                val username = currentUsernameForProfile(endpoint.profileKey)
                val password = currentPasswordForProfile(endpoint.profileKey)
                val client = ClipCascadeClient(
                    serverUrl = endpoint.address,
                    username = username,
                    password = password
                )
                clipCascadeClient = client

                try {
                    client.connect { data ->
                        markBackendActivity()
                        handleClipCascadeMessage(data)
                    }
                    markBackendActivity()
                    resetFailureState()
                    flushPendingUploads("clipcascade-connected")
                    val closeCause = client.awaitClose()
                    if (!isActive) {
                        return@launch
                    }

                    val error = closeCause as? Exception
                        ?: IOException("ClipCascade websocket disconnected")
                    if (handleConnectivityFailure(endpoint, error)) {
                        continue
                    }
                    delay(5000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (handleConnectivityFailure(endpoint, e)) {
                        continue
                    }
                    Log.e(TAG, "[ClipCascade] Connection loop error", e)
                    delay(5000)
                } finally {
                    if (clipCascadeClient === client) {
                        clipCascadeClient = null
                    }
                    client.close()
                }
            }
        }
    }

    private fun startOneClipSync() {
        disconnectOneClipClient()
        syncJob = scope.launch {
            while (isActive) {
                if (!shouldRunSyncLoops()) {
                    Log.d(TAG, "[OneClip] Sync loops are disabled, stop SSE loop")
                    break
                }
                val endpoint = currentEndpoint()
                if (endpoint.address.isBlank()) {
                    Log.d(TAG, "[OneClip] Server address is blank, skipping SSE sync")
                    return@launch
                }

                val client = OneClipEventClient(endpoint.address)
                oneClipClient = client

                try {
                    client.connect { event ->
                        markBackendActivity()
                        if (!event.update) {
                            return@connect
                        }
                        scope.launch {
                            runCatching { checkRemoteClipboard(forceRefresh = true) }
                                .onFailure { error ->
                                    Log.e(TAG, "[OneClip] Failed to refresh clipboard after SSE event", error)
                                }
                        }
                    }

                    checkRemoteClipboard()
                    markBackendActivity()
                    resetFailureState()
                    flushPendingUploads("oneclip-connected")

                    val closeCause = client.awaitClose()
                    if (!isActive) {
                        return@launch
                    }

                    val error = closeCause as? Exception
                        ?: IOException("OneClip SSE disconnected")
                    if (handleConnectivityFailure(endpoint, error)) {
                        continue
                    }
                    delay(5000)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (handleConnectivityFailure(endpoint, e)) {
                        continue
                    }
                    Log.e(TAG, "[OneClip] Connection loop error", e)
                    delay(5000)
                } finally {
                    if (oneClipClient === client) {
                        oneClipClient = null
                    }
                    client.close()
                }
            }
        }
    }

    private suspend fun checkRemoteClipboard(forceRefresh: Boolean = false) {
        remoteFetchMutex.withLock {
            importedProfileIdsInCurrentSync.clear()
            val endpoint = currentEndpoint()
            ensureEndpointState(endpoint)
            val url = endpoint.address
            val user = currentUsernameForProfile(endpoint.profileKey)
            val pass = currentPasswordForProfile(endpoint.profileKey)
            val backend = endpoint.backend

            if (url.isBlank()) return

            val downloadPath = prefs.getString("download_path", null)
            val downloadUri = StoragePathUtils.resolveDownloadUri(
                displayPath = downloadPath,
                storedUri = prefs.getString("download_path_uri", null)
            )

            val result = SyncClient.fetchClipboard(
                context = this,
                serverUrl = url,
                username = user,
                pass = pass,
                backend = backend,
                lastRevision = if (forceRefresh && backend == ServerBackend.ONECLIP) null else lastRemoteRevision,
                downloadDirUri = downloadUri,
                preDownloadFilter = ::shouldAcceptIncomingMetadata
            )
            noteRemoteSyncSuccess()

            val fetchedItems = result.items
            if (fetchedItems.isEmpty()) {
                result.revision?.let {
                    lastRemoteRevision = it
                    persistRemoteRevision(endpoint, it)
                }
                return
            }
            val acceptedItems = buildList {
                for (data in fetchedItems) {
                    if (!shouldAcceptIncomingClipboard(data)) {
                        Log.d(TAG, "[Pull] Incoming clipboard rejected by receive filter: type=${data.type} name=${data.dataName}")
                        continue
                    }
                    if (isSuppressedRemoteClipboard(data)) {
                        Log.d(TAG, "[Pull] Skipping locally suppressed remote clipboard item: type=${data.type} text=${data.text}")
                        continue
                    }
                    if (data.text.isBlank() && !data.type.equals("Text", ignoreCase = true)) {
                        Log.d(TAG, "[Pull] Skipping remote binary clipboard item without a readable local URI: type=${data.type} name=${data.dataName}")
                        continue
                    }
                    val profileId = data.id
                    if (profileId.isNotBlank() && profileId in importedProfileIdsInCurrentSync) {
                        Log.d(TAG, "[Pull] Skipping duplicate profileId: $profileId")
                        continue
                    }
                    if (profileId.isNotBlank()) {
                        importedProfileIdsInCurrentSync.add(profileId)
                    }
                    add(data)
                }
            }
            if (acceptedItems.isEmpty()) {
                result.revision?.let {
                    lastRemoteRevision = it
                    persistRemoteRevision(endpoint, it)
                }
                return
            }

            var importedAll = true
            acceptedItems.forEach { data ->
                val remoteText = data.text
                Log.d(TAG, "[Pull] Processed data: type=${data.type}, text=$remoteText")
                acknowledgePendingUploads(remoteText)
                if (shouldSkipRemoteImportForLocalEcho(data, remoteText)) {
                    Log.d(TAG, "[Pull] Skipping echoed local clipboard item in remote history")
                    return@forEach
                }
                importedAll = importRemoteClipboardEntry(data, downloadUri) && importedAll
            }

            if (endpoint.backend == ServerBackend.SYNCCLIPBOARD && !importedAll) {
                Log.d(TAG, "[Pull] Remote history import is not ready yet; keeping SyncClipboard revision unchanged")
                ensureRemoteBinding()
                return
            }

            val latestItem = acceptedItems.last()
            val remoteText = latestItem.text
            if (
                remoteText.isNotEmpty() &&
                remoteText != lastLocalContent &&
                remoteText != lastRemoteContent &&
                remoteText != lastUploadedContent
            ) {
                Log.d(TAG, "[Pull] Remote content changed, updating local")
                lastRemoteContent = remoteText
                lastLocalContent = remoteText
                lastUploadedContent = remoteText
                persistLastSyncedContent(remoteText)
                rememberIgnoredRemoteClipboardContent(remoteText)

                withContext(Dispatchers.Main) {
                    if (latestItem.type.equals("Text", ignoreCase = true)) {
                        updateSystemClipboard(remoteText)
                    } else {
                        updateSystemClipboardWithUri(Uri.parse(remoteText), downloadUri)
                    }
                }
            }

            result.revision?.let {
                lastRemoteRevision = it
                persistRemoteRevision(endpoint, it)
            }
        }
    }

    private fun importRemoteClipboardEntry(data: ClipboardData, downloadUri: Uri?): Boolean {
        val remoteService = connection?.remoteService ?: return false
        val remoteText = data.text.takeIf { it.isNotBlank() } ?: return false
        grantRemoteClipboardPermissions(remoteText, downloadUri)
        return runCatching {
            remoteService.importRemoteClipboardEntry(
                remoteText,
                if (remoteText.startsWith("content://") || remoteText.startsWith("file://")) remoteText else "",
                downloadUri?.toString().orEmpty(),
                importedClipboardMimeType(data),
                data.remoteTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                false
            )
        }.onFailure { error ->
            Log.w(TAG, "[Pull] Failed to import remote clipboard entry into Fcitx history", error)
        }.isSuccess
    }

    private fun grantRemoteClipboardPermissions(remoteText: String, rootUri: Uri?) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        remoteText.takeIf { it.startsWith("content://") }
            ?.let(Uri::parse)
            ?.let { uri ->
                runCatching {
                    grantUriPermission(BuildConfig.APPLICATION_ID, uri, flags)
                }.onFailure { error ->
                    Log.w(TAG, "[Pull] Failed to grant remote clipboard URI permission to main app: $uri", error)
                }
            }
        rootUri?.let { uri ->
            runCatching {
                grantUriPermission(BuildConfig.APPLICATION_ID, uri, flags)
            }.onFailure { error ->
                Log.w(TAG, "[Pull] Failed to grant remote clipboard root permission to main app: $uri", error)
            }
        }
    }

    private fun importedClipboardMimeType(data: ClipboardData): String {
        data.mimeType.takeIf { it.isNotBlank() }?.let { return it }
        if (data.type.equals("Text", ignoreCase = true)) {
            return ClipDescription.MIMETYPE_TEXT_PLAIN
        }
        val text = data.text
        if (text.startsWith("content://") || text.startsWith("file://")) {
            return runCatching { contentResolver.getType(Uri.parse(text)) }.getOrNull()
                ?: ClipDescription.MIMETYPE_TEXT_URILIST
        }
        return ClipDescription.MIMETYPE_TEXT_URILIST
    }

    private fun updateSystemClipboard(text: String) {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SyncClipboard", text).also(::markRemoteClip)
            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "[Pull] System clipboard updated (Text)")
        } catch (e: Exception) {
            Log.e(TAG, "[Pull] Failed to update system clipboard", e)
        }
    }

    private fun updateSystemClipboardWithUri(uri: Uri, rootUri: Uri? = null) {
        try {
            grantRemoteClipboardPermissions(uri.toString(), rootUri)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newUri(contentResolver, "SyncClipboard", uri).also {
                markRemoteClip(it, rootUri)
            }
            clipboard.setPrimaryClip(clip)
            Log.d(TAG, "[Pull] System clipboard updated with URI: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "[Pull] Failed to update system clipboard with URI", e)
        }
    }

    private fun markRemoteClip(clip: ClipData, rootUri: Uri? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        clip.description.extras = PersistableBundle().apply {
            putString(ClipboardMetadata.EXTRA_SOURCE, ClipboardMetadata.SOURCE_REMOTE)
            rootUri?.toString()?.takeIf { it.isNotBlank() }?.let {
                putString(ClipboardMetadata.EXTRA_REMOTE_ROOT_URI, it)
            }
        }
    }

    private fun handleSystemClipboardChanged() {
        val content = readClipboardContent() ?: return
        handleLocalClipboardUpdate(content, "system-clipboard")
    }

    private fun handleClipCascadeMessage(data: ClipCascadeClipboardData) {
        val normalizedType = data.type.lowercase(Locale.ROOT)
        val payloadFingerprint = buildClipCascadePayloadFingerprint(data)
        Log.d(TAG, "[ClipCascade] Received remote payload type=$normalizedType size=${data.payload.length}")
        if (payloadFingerprint.isEmpty() ||
            payloadFingerprint == lastLocalContent ||
            payloadFingerprint == lastRemoteContent ||
            payloadFingerprint == lastUploadedContent
        ) {
            return
        }

        scope.launch {
            when (normalizedType) {
                "text" -> {
                    if (!shouldAcceptIncomingText(data.payload)) {
                        Log.d(TAG, "[ClipCascade] Rejected text payload by receive filter")
                        return@launch
                    }
                    acknowledgePendingUploads(data.payload)
                    rememberAcceptedRemotePayload(payloadFingerprint)
                    withContext(Dispatchers.Main) {
                        updateSystemClipboard(data.payload)
                    }
                }

                "image" -> handleClipCascadeImage(data, payloadFingerprint)

                "file_eager" -> handleClipCascadeFileEager(data, payloadFingerprint)

                "file_stub" -> {
                    if (!shouldAcceptIncomingBinary(data.filename, null)) {
                        Log.d(TAG, "[ClipCascade] Rejected file placeholder by receive filter")
                        return@launch
                    }
                    rememberAcceptedRemotePayload(payloadFingerprint)
                    withContext(Dispatchers.Main) {
                        updateSystemClipboard(buildClipCascadeFileStubSummary(data))
                    }
                }

                else -> Log.w(TAG, "[ClipCascade] Ignoring unsupported payload type: ${data.type}")
            }
        }
    }

    private suspend fun handleClipCascadeImage(data: ClipCascadeClipboardData, payloadFingerprint: String) {
        val bytes = runCatching { Base64.decode(data.payload, Base64.DEFAULT) }
            .getOrElse { error ->
                Log.e(TAG, "[ClipCascade] Failed to decode image payload", error)
                return
            }
        val downloadUri = resolveDownloadUri()
        val fileName = SyncClient.buildClipCascadeImageFileName(data.filename)
        if (!shouldAcceptIncomingBinary(fileName, bytes.size.toLong())) {
            Log.d(TAG, "[ClipCascade] Rejected image payload by receive filter: $fileName")
            return
        }
        val mimeType = SyncClient.guessClipCascadeImageMimeType(fileName)
        val savedUri = SyncClient.saveIncomingBytes(
            context = this,
            dirUri = downloadUri,
            fileName = fileName,
            bytes = bytes,
            mimeType = mimeType
        )
        if (savedUri != null) {
            rememberAcceptedRemotePayload(payloadFingerprint)
            withContext(Dispatchers.Main) {
                updateSystemClipboardWithUri(savedUri, downloadUri)
            }
        }
    }

    private suspend fun handleClipCascadeFileEager(data: ClipCascadeClipboardData, payloadFingerprint: String) {
        val bytes = runCatching { Base64.decode(data.payload, Base64.DEFAULT) }
            .getOrElse { error ->
                Log.e(TAG, "[ClipCascade] Failed to decode file payload", error)
                return
            }
        val downloadUri = resolveDownloadUri()
        val fileName = data.filename
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: "ClipCascade-${System.currentTimeMillis()}.bin"
        if (!shouldAcceptIncomingBinary(fileName, bytes.size.toLong())) {
            Log.d(TAG, "[ClipCascade] Rejected file payload by receive filter: $fileName")
            return
        }
        val savedUri = SyncClient.saveIncomingBytes(
            context = this,
            dirUri = downloadUri,
            fileName = fileName,
            bytes = bytes
        )
        if (savedUri != null) {
            rememberAcceptedRemotePayload(payloadFingerprint)
            withContext(Dispatchers.Main) {
                updateSystemClipboardWithUri(savedUri, downloadUri)
            }
        }
    }

    private fun buildClipCascadePayloadFingerprint(data: ClipCascadeClipboardData): String {
        return when (data.type.lowercase(Locale.ROOT)) {
            "text" -> data.payload
            else -> "${data.type}:${data.filename.orEmpty()}:${data.payload.hashCode()}"
        }
    }

    private fun buildClipCascadeFileStubSummary(data: ClipCascadeClipboardData): String {
        val firstName = data.filename
            ?.takeIf { it.isNotBlank() }
            ?: data.payload.lineSequence().firstOrNull()?.trim().orEmpty()
        return if (firstName.isNotBlank()) {
            "ClipCascade file placeholder: $firstName"
        } else {
            "ClipCascade file placeholder received"
        }
    }

    private fun resolveDownloadUri(): Uri? {
        val downloadPath = prefs.getString("download_path", null)
        return StoragePathUtils.resolveDownloadUri(
            displayPath = downloadPath,
            storedUri = prefs.getString("download_path_uri", null)
        )
    }

    private fun readClipboardContent(): String? {
        val clip = runCatching { clipboardManager.primaryClip }
            .onFailure { error ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastClipboardReadFailureLoggedAt >= 60_000L) {
                    lastClipboardReadFailureLoggedAt = now
                    Log.w(
                        TAG,
                        "[Push] Failed to read system clipboard. On Android 10+, non-default-IME background access can be blocked by platform policy.",
                        error
                    )
                }
            }
            .getOrNull()
            ?: return null
        if (clip.itemCount == 0) return null
        val item = clip.getItemAt(0)
        item.uri?.toString()?.let { return it }
        item.text?.toString()?.takeIf { it.isNotEmpty() }?.let { return it }
        return item.coerceToText(this)?.toString()?.takeIf { it.isNotEmpty() }
    }

    private fun registerClipboardListenerIfNeeded() {
        if (clipboardListenerRegistered) return
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        clipboardListenerRegistered = true
    }

    private fun unregisterClipboardListenerIfNeeded() {
        if (!clipboardListenerRegistered) return
        runCatching { clipboardManager.removePrimaryClipChangedListener(clipboardListener) }
        clipboardListenerRegistered = false
    }

    private fun registerPrefsListenerIfNeeded() {
        if (prefsListenerRegistered) return
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        prefsListenerRegistered = true
    }

    private fun unregisterPrefsListenerIfNeeded() {
        if (!prefsListenerRegistered) return
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        prefsListenerRegistered = false
    }

    private fun registerScreenStateReceiverIfNeeded() {
        if (screenStateReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(screenStateReceiver, filter)
            }
            screenStateReceiverRegistered = true
        }.onFailure {
            Log.w(TAG, "Failed to register screen-state receiver", it)
        }
    }

    private fun unregisterScreenStateReceiverIfNeeded() {
        if (!screenStateReceiverRegistered) return
        runCatching {
            unregisterReceiver(screenStateReceiver)
        }.onFailure {
            Log.w(TAG, "Failed to unregister screen-state receiver", it)
        }
        screenStateReceiverRegistered = false
    }

    private fun ensureScope() {
        if (!scope.isActive) {
            scope = createScope()
        }
    }

    private fun ensureRemoteBinding(forceRebind: Boolean = false) {
        if (!serviceRunning) return
        if (!forceRebind && transformerRegistered && connection?.remoteService != null) {
            return
        }

        val previousConnection = connection
        connection = null
        if (previousConnection != null) {
            runCatching { unbindService(previousConnection) }
        }

        val sessionId = ++connectionSessionId
        connection = bindFcitxRemoteService(
            BuildConfig.APPLICATION_ID,
            onDisconnect = {
                if (sessionId != connectionSessionId) return@bindFcitxRemoteService
                Log.d(TAG, "Disconnected from Fcitx")
                transformerRegistered = false
                scope.launch {
                    delay(1000)
                    if (serviceRunning) {
                        ensureRemoteBinding(forceRebind = true)
                    }
                }
            },
            onConnected = { service ->
                if (sessionId != connectionSessionId || !serviceRunning) {
                    return@bindFcitxRemoteService
                }
                Log.d(TAG, "Connected to Fcitx")
                runCatching {
                    service.registerClipboardEntryTransformer(transformer)
                }.onSuccess {
                    transformerRegistered = true
                    Log.d(TAG, "Clipboard transformer registered")
                    if (currentEndpoint().backend == ServerBackend.SYNCCLIPBOARD) {
                        scope.launch {
                            runCatching { checkRemoteClipboard() }
                                .onFailure { error ->
                                    Log.w(TAG, "Failed to refresh SyncClipboard after Fcitx IPC connected", error)
                                }
                        }
                    }
                }.onFailure { error ->
                    transformerRegistered = false
                    Log.e(TAG, "Failed to register transformer; pull sync will continue", error)
                }
            }
        )
    }

    private fun registerNetworkCallbackIfNeeded() {
        if (networkCallbackRegistered) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, networkCallback)
            }
            networkCallbackRegistered = true
        }.onFailure {
            Log.w(TAG, "Failed to register network callback", it)
        }
    }

    private fun unregisterNetworkCallbackIfNeeded() {
        if (!networkCallbackRegistered) return
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }.onFailure {
            Log.w(TAG, "Failed to unregister network callback", it)
        }
        networkCallbackRegistered = false
    }

    private fun handleActionIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PAUSE_SYNC -> {
                prefs.edit()
                    .putBoolean(PREF_QUICK_SYNC, false)
                    .putBoolean(PREF_QUICK_SYNC_UNREACHABLE, false)
                    .apply()
                QuickSyncTileService.requestTileRefresh(this)
            }

            ACTION_RECONNECT_SYNC -> {
                scheduleReconnect("notification-reconnect", immediate = true)
            }

            ACTION_START_SYNC,
            null -> {
                if (intent?.getBooleanExtra(EXTRA_FORCE_ENABLE_SYNC, false) == true) {
                    prefs.edit()
                        .putBoolean(PREF_QUICK_SYNC, true)
                        .putBoolean(PREF_QUICK_SYNC_UNREACHABLE, false)
                        .apply()
                    QuickSyncTileService.requestTileRefresh(this)
                }
            }

            ACTION_INGEST_CAPTURED_CLIPBOARD -> {
                val content = intent.getStringExtra(EXTRA_CAPTURED_CLIPBOARD_CONTENT).orEmpty()
                if (content.isNotBlank()) {
                    forceUploadClipboard(content, "manual-capture")
                }
            }

            ACTION_SUPPRESS_REMOTE_CLIPBOARD -> {
                val contents = intent.getStringArrayListExtra(EXTRA_SUPPRESSED_REMOTE_ITEMS).orEmpty()
                if (contents.isNotEmpty()) {
                    suppressRemoteClipboardContents(contents)
                }
            }
        }
    }

    private fun scheduleReconnect(reason: String, immediate: Boolean = false) {
        if (!shouldRunSyncLoops()) return
        networkReconnectJob?.cancel()
        networkReconnectJob = scope.launch {
            if (!immediate) {
                delay(NETWORK_RECONNECT_DEBOUNCE_MS)
            }
            Log.d(TAG, "[Reconnect] Restarting sync runtime: $reason")
            startPeriodicSync()
            startHealthMonitor()
        }
    }

    private fun updateForegroundState() {
        if (shouldRunInForeground()) {
            startForegroundCompat()
        } else {
            stopForegroundState()
        }
    }

    private fun shouldRunInForeground(): Boolean {
        return false
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.keep_alive_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.keep_alive_notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundCompat() {
        val notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            foregroundActive = true
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is android.app.ForegroundServiceStartNotAllowedException
            ) {
                Log.w(
                    TAG,
                    "[Service] Foreground service start not allowed; system is likely in punishment state for dataSync. Running without foreground notification.",
                    e
                )
            } else {
                throw e
            }
            foregroundActive = false
        }
    }

    private fun stopForegroundState() {
        if (!foregroundActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        foregroundActive = false
    }

    private fun buildForegroundNotification() = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle(getString(R.string.keep_alive_notification_title))
        .setContentText(buildForegroundNotificationText())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                1,
                Intent(this, ClipboardSyncSettingsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            android.R.drawable.ic_popup_sync,
            getString(R.string.keep_alive_notification_reconnect),
            PendingIntent.getService(
                this,
                2,
                Intent(this, MainService::class.java).apply { action = ACTION_RECONNECT_SYNC },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            android.R.drawable.ic_media_pause,
            getString(R.string.keep_alive_notification_pause),
            PendingIntent.getService(
                this,
                3,
                Intent(this, MainService::class.java).apply { action = ACTION_PAUSE_SYNC },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            android.R.drawable.ic_menu_send,
            getString(R.string.keep_alive_notification_capture),
            PendingIntent.getActivity(
                this,
                4,
                Intent(this, ClipboardCaptureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun buildForegroundNotificationText(): String {
        val backendLabel = when (currentBackend()) {
            ServerBackend.SYNCCLIPBOARD -> getString(R.string.server_profile_syncclipboard)
            ServerBackend.ONECLIP -> getString(R.string.server_profile_oneclip)
            ServerBackend.CLIPCASCADE -> getString(R.string.server_profile_clipcascade)
        }
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val ignoringBatteryOptimization = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(packageName) == true
        } else {
            true
        }
        return if (ignoringBatteryOptimization) {
            getString(R.string.keep_alive_notification_text, backendLabel)
        } else {
            getString(R.string.keep_alive_notification_text_battery, backendLabel)
        }
    }

    private fun noteRemoteSyncSuccess() {
        lastSuccessfulRemoteSyncAt = SystemClock.elapsedRealtime()
        markBackendActivity()
    }

    private fun markBackendActivity() {
        lastBackendActivityAt = SystemClock.elapsedRealtime()
    }

    private fun rememberAcceptedRemotePayload(payloadFingerprint: String) {
        lastRemoteContent = payloadFingerprint
        lastLocalContent = payloadFingerprint
        lastUploadedContent = payloadFingerprint
        persistLastSyncedContent(payloadFingerprint)
        rememberIgnoredRemoteClipboardContent(payloadFingerprint)
        noteRemoteSyncSuccess()
    }

    private fun rememberIgnoredRemoteClipboardContent(content: String) {
        if (content.isBlank()) return
        synchronized(ignoredRemoteClipboardContents) {
            ignoredRemoteClipboardContents.remove(content)
            ignoredRemoteClipboardContents.add(content)
            while (ignoredRemoteClipboardContents.size > 8) {
                val first = ignoredRemoteClipboardContents.firstOrNull() ?: break
                ignoredRemoteClipboardContents.remove(first)
            }
        }
    }

    private fun consumeIgnoredRemoteClipboardContent(content: String): Boolean {
        synchronized(ignoredRemoteClipboardContents) {
            return ignoredRemoteClipboardContents.remove(content)
        }
    }

    private fun suppressRemoteClipboardContents(contents: Collection<String>) {
        val normalized = contents
            .asSequence()
            .map(String::trim)
            .filter { it.startsWith("content://") || it.startsWith("file://") }
            .distinct()
            .toList()
        if (normalized.isEmpty()) return
        synchronized(suppressedRemoteClipboardContents) {
            normalized.forEach { content ->
                suppressedRemoteClipboardContents.remove(content)
                suppressedRemoteClipboardContents.add(content)
            }
            while (suppressedRemoteClipboardContents.size > MAX_SUPPRESSED_REMOTE_ITEMS) {
                val first = suppressedRemoteClipboardContents.firstOrNull() ?: break
                suppressedRemoteClipboardContents.remove(first)
            }
        }
        persistSuppressedRemoteClipboardContents()
    }

    private fun isSuppressedRemoteClipboard(data: ClipboardData): Boolean {
        val remoteText = data.text
            .takeIf { it.startsWith("content://") || it.startsWith("file://") }
            ?: return false
        synchronized(suppressedRemoteClipboardContents) {
            return remoteText in suppressedRemoteClipboardContents
        }
    }

    private fun disconnectClipCascadeClient() {
        clipCascadeClient?.close()
        clipCascadeClient = null
    }

    private fun disconnectOneClipClient() {
        oneClipClient?.close()
        oneClipClient = null
    }

    private fun ensureSelfStarted() {
        if (selfStarted) return
        runCatching {
            startSyncService(this, "self-start")
            selfStarted = true
            Log.d(TAG, "MainService promoted to started service")
        }.onFailure {
            Log.w(TAG, "Failed to self-start MainService; background sync may depend on IME binding", it)
        }
    }

    private fun resetRemoteCache() {
        activeEndpointIdentity = null
        lastRemoteRevision = null
        lastRemoteContent = null
    }

    private fun resetFailureState() {
        if (prefs.getBoolean(PREF_QUICK_SYNC_UNREACHABLE, false)) {
            prefs.edit().putBoolean(PREF_QUICK_SYNC_UNREACHABLE, false).apply()
        }
    }

    private fun currentBackend(): ServerBackend {
        return ServerBackend.fromProfileType(
            prefs.getString(SERVER_PROFILE_TYPE_KEY, null)
        )
    }

    private fun shouldRunSyncLoops(): Boolean {
        val quickSyncEnabled = prefs.getBoolean(PREF_QUICK_SYNC, DEFAULT_QUICK_SYNC_ENABLED)
        val imeSyncActive = prefs.getBoolean(PREF_IME_SYNC_ACTIVE, false)
        return quickSyncEnabled && imeSyncActive && isScreenInteractive()
    }

    private fun isScreenInteractive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }

    private fun isPowerSaveEnabled(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && powerManager.isPowerSaveMode
    }

    private fun currentRuntimeMode(): RuntimeMode {
        return when {
            isPowerSaveEnabled() -> RuntimeMode.POWER_SAVE
            !isScreenInteractive() -> RuntimeMode.SCREEN_OFF
            else -> RuntimeMode.NORMAL
        }
    }

    private fun currentPollingIntervalSeconds(baseIntervalSeconds: Long): Long {
        return when (currentRuntimeMode()) {
            RuntimeMode.NORMAL -> baseIntervalSeconds
            RuntimeMode.SCREEN_OFF -> maxOf(baseIntervalSeconds, SCREEN_OFF_POLL_INTERVAL_SECONDS)
            RuntimeMode.POWER_SAVE -> maxOf(baseIntervalSeconds, POWER_SAVE_POLL_INTERVAL_SECONDS)
            RuntimeMode.AGGRESSIVE -> maxOf(baseIntervalSeconds, AGGRESSIVE_POLL_INTERVAL_SECONDS)
        }
    }

    private fun currentHealthCheckDelayMs(): Long {
        return when (currentRuntimeMode()) {
            RuntimeMode.NORMAL -> EVENT_BACKEND_HEALTH_CHECK_MS
            RuntimeMode.SCREEN_OFF -> SCREEN_OFF_HEALTH_CHECK_MS
            RuntimeMode.POWER_SAVE -> POWER_SAVE_HEALTH_CHECK_MS
            RuntimeMode.AGGRESSIVE -> AGGRESSIVE_HEALTH_CHECK_MS
        }
    }

    private fun currentFallbackPullDelayMs(): Long {
        return when (currentRuntimeMode()) {
            RuntimeMode.NORMAL -> EVENT_BACKEND_FALLBACK_PULL_MS
            RuntimeMode.SCREEN_OFF -> SCREEN_OFF_FALLBACK_PULL_MS
            RuntimeMode.POWER_SAVE -> POWER_SAVE_FALLBACK_PULL_MS
            RuntimeMode.AGGRESSIVE -> AGGRESSIVE_FALLBACK_PULL_MS
        }
    }

    private fun currentStaleReconnectDelayMs(): Long {
        return when (currentRuntimeMode()) {
            RuntimeMode.NORMAL -> EVENT_BACKEND_STALE_RECONNECT_MS
            RuntimeMode.SCREEN_OFF -> SCREEN_OFF_STALE_RECONNECT_MS
            RuntimeMode.POWER_SAVE -> POWER_SAVE_STALE_RECONNECT_MS
            RuntimeMode.AGGRESSIVE -> AGGRESSIVE_STALE_RECONNECT_MS
        }
    }

    private fun currentReceiveFilter(): ReceiveFilter {
        val state = SyncFilterPrefs.loadState(prefs)
        if (!state.hasActiveRule) {
            return ReceiveFilter(
                blockedExtensions = emptySet(),
                minFileSizeBytes = null,
                maxFileSizeBytes = null,
                minTextChars = null,
                maxTextChars = null
            )
        }
        return ReceiveFilter(
            blockedExtensions = state.blockedExtensions,
            minFileSizeBytes = null,
            maxFileSizeBytes = state.maxFileSizeBytes,
            minTextChars = state.minTextChars,
            maxTextChars = state.maxTextChars
        ).normalized()
    }

    private fun shouldAcceptIncomingMetadata(data: ClipboardData): Boolean {
        return if (isBinaryClipboardData(data)) {
            shouldAcceptIncomingBinary(
                fileName = inferIncomingFileName(data),
                sizeBytes = data.size.takeIf { it > 0 }
            )
        } else {
            shouldAcceptIncomingText(data.text)
        }
    }

    private fun shouldAcceptIncomingClipboard(data: ClipboardData): Boolean {
        return if (isBinaryClipboardData(data)) {
            shouldAcceptIncomingBinary(
                fileName = inferIncomingFileName(data),
                sizeBytes = data.size.takeIf { it > 0 }
            )
        } else {
            shouldAcceptIncomingText(data.text)
        }
    }

    private fun shouldAcceptIncomingText(text: String): Boolean {
        val filter = currentReceiveFilter()
        val length = text.codePointCount(0, text.length)
        filter.minTextChars?.let { minChars ->
            if (length < minChars) {
                return false
            }
        }
        filter.maxTextChars?.let { maxChars ->
            if (length > maxChars) {
                return false
            }
        }
        return true
    }

    private fun shouldAcceptIncomingBinary(fileName: String?, sizeBytes: Long?): Boolean {
        val filter = currentReceiveFilter()
        val extension = fileName
            ?.substringAfterLast('.', "")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
        if (extension.isNotEmpty() && extension in filter.blockedExtensions) {
            return false
        }
        sizeBytes?.takeIf { it > 0 }?.let { actualSize ->
            filter.minFileSizeBytes?.let { minSize ->
                if (actualSize < minSize) {
                    return false
                }
            }
            filter.maxFileSizeBytes?.let { maxSize ->
                if (actualSize > maxSize) {
                    return false
                }
            }
        }
        return true
    }

    private fun isBinaryClipboardData(data: ClipboardData): Boolean {
        return !data.type.equals("text", ignoreCase = true) ||
            data.hasData ||
            data.hasImage ||
            data.dataName.isNotBlank()
    }

    private fun inferIncomingFileName(data: ClipboardData): String? {
        if (data.dataName.isNotBlank()) {
            return data.dataName
        }
        if (data.type.equals("text", ignoreCase = true)) {
            return null
        }
        return data.text
            .substringAfterLast('/')
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
    }

    private suspend fun handleConnectivityFailure(
        endpoint: ServerEndpoint,
        cause: Exception
    ): Boolean {
        Log.e(TAG, "[Pull] Error checking remote clipboard for ${endpoint.profileKey}@${endpoint.address}", cause)

        if (verifyEndpointRecovered(endpoint)) {
            Log.d(TAG, "[Pull] Endpoint recovered during connectivity retry: ${endpoint.address}")
            resetFailureState()
            return true
        }

        if (switchToReachableEndpoint(endpoint)) {
            Log.w(TAG, "[Pull] Switched active endpoint away from ${endpoint.address}")
            resetRemoteCache()
            resetFailureState()
            return true
        }

        Log.e(TAG, "[Pull] All configured endpoints are unreachable, keeping quick sync enabled but marking it unavailable")
        prefs.edit().putBoolean(PREF_QUICK_SYNC_UNREACHABLE, true).apply()
        stopPeriodicSync()
        return true
    }

    private suspend fun verifyEndpointRecovered(endpoint: ServerEndpoint): Boolean {
        val username = currentUsernameForProfile(endpoint.profileKey)
        val password = currentPasswordForProfile(endpoint.profileKey)

        CONNECTIVITY_RETRY_DELAYS_MS.forEach { delayMs ->
            delay(delayMs)
            val result = SyncClient.testConnection(
                serverUrl = endpoint.address,
                username = username,
                pass = password,
                backend = endpoint.backend
            )
            if (result.isSuccess) {
                return true
            }
        }
        return false
    }

    private suspend fun switchToReachableEndpoint(failedEndpoint: ServerEndpoint): Boolean {
        val alternatives = endpointCandidates()
            .filter { it.identity != failedEndpoint.identity }

        for (candidate in alternatives) {
            val username = currentUsernameForProfile(candidate.profileKey)
            val password = currentPasswordForProfile(candidate.profileKey)
            val result = SyncClient.testConnection(
                serverUrl = candidate.address,
                username = username,
                pass = password,
                backend = candidate.backend
            )
            if (result.isSuccess) {
                prefs.edit()
                    .putString(SERVER_PROFILE_TYPE_KEY, candidate.profileKey)
                    .putString(SERVER_ADDRESS_KEY, candidate.address)
                    .putString(PREF_USERNAME, username)
                    .putString(PREF_PASSWORD, password)
                    .apply()
                return true
            }
        }
        return false
    }

    private fun currentEndpoint(): ServerEndpoint {
        val profileKey = prefs.getString(SERVER_PROFILE_TYPE_KEY, PROFILE_SYNC_CLIPBOARD)
            ?.takeIf { it.isNotBlank() }
            ?: PROFILE_SYNC_CLIPBOARD
        val address = prefs.getString(SERVER_ADDRESS_KEY, null)
            ?.trim()
            .orEmpty()
            .ifBlank { storedAddressForProfile(profileKey) }
        return ServerEndpoint(
            profileKey = profileKey,
            address = address,
            backend = ServerBackend.fromProfileType(profileKey)
        )
    }

    private fun endpointCandidates(): List<ServerEndpoint> {
        val current = currentEndpoint()
        val orderedProfiles = buildList {
            add(current.profileKey)
            add(PROFILE_SYNC_CLIPBOARD)
            add(PROFILE_ONE_CLIP)
            add(PROFILE_CLIP_CASCADE)
            add(PROFILE_CUSTOM)
        }.distinct()

        return orderedProfiles.mapNotNull { profileKey ->
            val address = if (profileKey == current.profileKey) {
                current.address
            } else {
                storedAddressForProfile(profileKey)
            }.trim()

            if (address.isBlank()) {
                null
            } else {
                ServerEndpoint(
                    profileKey = profileKey,
                    address = address,
                    backend = ServerBackend.fromProfileType(profileKey)
                )
            }
        }.distinctBy { it.identity }
    }

    private fun storedAddressForProfile(profileKey: String): String {
        val key = when (profileKey) {
            PROFILE_SYNC_CLIPBOARD -> SERVER_ADDRESS_SYNC_CLIPBOARD_KEY
            PROFILE_ONE_CLIP -> SERVER_ADDRESS_ONE_CLIP_KEY
            PROFILE_CLIP_CASCADE -> SERVER_ADDRESS_CLIP_CASCADE_KEY
            PROFILE_CUSTOM -> SERVER_ADDRESS_CUSTOM_KEY
            else -> null
        }
        val stored = key?.let { prefs.getString(it, null) }?.trim().orEmpty()
        if (stored.isNotEmpty()) return stored
        return when (profileKey) {
            PROFILE_SYNC_CLIPBOARD -> DEFAULT_SYNC_CLIPBOARD_URL
            PROFILE_ONE_CLIP -> DEFAULT_ONE_CLIP_URL
            PROFILE_CLIP_CASCADE -> DEFAULT_CLIP_CASCADE_URL
            else -> ""
        }
    }

    private fun currentUsernameForProfile(profileKey: String): String {
        val key = when (profileKey) {
            PROFILE_SYNC_CLIPBOARD -> "username_syncclipboard"
            PROFILE_ONE_CLIP -> "username_oneclip"
            PROFILE_CLIP_CASCADE -> "username_clipcascade"
            PROFILE_CUSTOM -> "username_custom"
            else -> null
        }
        val stored = key?.let { prefs.getString(it, null) }.orEmpty()
        return if (stored.isNotBlank() || profileKey == PROFILE_ONE_CLIP) {
            stored
        } else if (profileKey == PROFILE_CLIP_CASCADE || profileKey == PROFILE_SYNC_CLIPBOARD || profileKey == PROFILE_CUSTOM) {
            "admin"
        } else {
            ""
        }
    }

    private fun currentPasswordForProfile(profileKey: String): String {
        val key = when (profileKey) {
            PROFILE_SYNC_CLIPBOARD -> "password_syncclipboard"
            PROFILE_ONE_CLIP -> "password_oneclip"
            PROFILE_CLIP_CASCADE -> "password_clipcascade"
            PROFILE_CUSTOM -> "password_custom"
            else -> null
        }
        val stored = key?.let { prefs.getString(it, null) }.orEmpty()
        return if (stored.isNotBlank() || profileKey == PROFILE_ONE_CLIP) {
            stored
        } else when (profileKey) {
            PROFILE_ONE_CLIP -> ""
            else -> "admin123"
        }
    }

    private data class ServerEndpoint(
        val profileKey: String,
        val address: String,
        val backend: ServerBackend
    ) {
        val identity: String
            get() = "$profileKey|${address.trim()}"
    }

    @Serializable
    private data class PendingUploadEntry(
        val content: String,
        val enqueuedAt: Long = System.currentTimeMillis()
    )

    private fun loadPersistentSyncState() {
        pendingUploads.clear()
        prefs.getString(PREF_PENDING_UPLOADS, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { serialized ->
                runCatching {
                    stateJson.decodeFromString<List<PendingUploadEntry>>(serialized)
                }.onSuccess { restored ->
                    pendingUploads += restored
                }.onFailure { error ->
                    Log.w(TAG, "[State] Failed to restore pending uploads", error)
                }
            }

        storedRemoteRevisions.clear()
        prefs.getString(PREF_REMOTE_REVISIONS, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { serialized ->
                runCatching {
                    stateJson.decodeFromString<Map<String, String>>(serialized)
                }.onSuccess { restored ->
                    storedRemoteRevisions.putAll(restored)
                }.onFailure { error ->
                    Log.w(TAG, "[State] Failed to restore remote revisions", error)
                }
            }

        suppressedRemoteClipboardContents.clear()
        prefs.getString(PREF_SUPPRESSED_REMOTE_ITEMS, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { serialized ->
                runCatching {
                    stateJson.decodeFromString<List<String>>(serialized)
                }.onSuccess { restored ->
                    restored.forEach { item ->
                        if (item.startsWith("content://") || item.startsWith("file://")) {
                            suppressedRemoteClipboardContents.add(item)
                        }
                    }
                }.onFailure { error ->
                    Log.w(TAG, "[State] Failed to restore suppressed remote clipboard items", error)
                }
            }
    }

    private fun persistPendingUploadsLocked() {
        prefs.edit()
            .putString(PREF_PENDING_UPLOADS, stateJson.encodeToString(pendingUploads))
            .apply()
    }

    private fun persistRemoteRevisions() {
        prefs.edit()
            .putString(PREF_REMOTE_REVISIONS, stateJson.encodeToString(storedRemoteRevisions))
            .apply()
    }

    private fun persistSuppressedRemoteClipboardContents() {
        prefs.edit()
            .putString(
                PREF_SUPPRESSED_REMOTE_ITEMS,
                stateJson.encodeToString(suppressedRemoteClipboardContents.toList())
            )
            .apply()
    }

    private fun persistLastSyncedContent(content: String) {
        prefs.edit()
            .putString(PREF_LAST_SYNCED_CONTENT, content)
            .apply()
    }

    private suspend fun enqueuePendingUpload(content: String): Boolean {
        return pendingUploadMutex.withLock {
            if (content.isEmpty()) {
                return@withLock false
            }
            pendingUploads.removeAll { it.content == content }
            pendingUploads += PendingUploadEntry(content = content)
            val overflow = pendingUploads.size - MAX_PENDING_UPLOADS
            if (overflow > 0) {
                repeat(overflow) {
                    pendingUploads.removeAt(0)
                }
            }
            persistPendingUploadsLocked()
            true
        }
    }

    private suspend fun acknowledgePendingUploads(content: String): Boolean {
        if (content.isBlank()) return false
        return pendingUploadMutex.withLock {
            val matchedIndex = pendingUploads.indexOfLast { it.content == content }
            if (matchedIndex < 0) {
                return@withLock false
            }
            repeat(matchedIndex + 1) {
                pendingUploads.removeAt(0)
            }
            persistPendingUploadsLocked()
            true
        }
    }

    private fun shouldSkipRemoteImportForLocalEcho(
        data: ClipboardData,
        remoteText: String
    ): Boolean {
        if (!data.type.equals("Text", ignoreCase = true)) return false
        if (remoteText.isBlank()) return false
        return remoteText == lastLocalContent || remoteText == lastUploadedContent
    }

    private fun ensureEndpointState(endpoint: ServerEndpoint) {
        if (activeEndpointIdentity == endpoint.identity) {
            return
        }
        activeEndpointIdentity = endpoint.identity
        lastRemoteRevision = storedRemoteRevisions[endpoint.identity]
        lastRemoteContent = null
    }

    private fun persistRemoteRevision(endpoint: ServerEndpoint, revision: String) {
        storedRemoteRevisions[endpoint.identity] = revision
        persistRemoteRevisions()
    }

    private suspend fun flushPendingUploads(reason: String, force: Boolean = false) {
        if (!force && !prefs.getBoolean(PREF_QUICK_SYNC, DEFAULT_QUICK_SYNC_ENABLED)) {
            return
        }
        pendingUploadDrainMutex.withLock {
            while (true) {
                val next = pendingUploadMutex.withLock {
                    pendingUploads.firstOrNull()
                } ?: return

                try {
                    pushClipboardToCloud(next.content)
                    pendingUploadMutex.withLock {
                        if (pendingUploads.firstOrNull() == next) {
                            pendingUploads.removeAt(0)
                        } else {
                            pendingUploads.removeAll { it.content == next.content }
                        }
                        persistPendingUploadsLocked()
                    }
                    Log.d(TAG, "[Push] Uploaded queued clipboard item from $reason")
                } catch (e: SyncClient.StaleClipboardContentException) {
                    Log.w(TAG, "[Push] Dropping stale clipboard item from queue: ${next.content}", e)
                    pendingUploadMutex.withLock {
                        if (pendingUploads.firstOrNull() == next) {
                            pendingUploads.removeAt(0)
                        } else {
                            pendingUploads.removeAll { it.content == next.content }
                        }
                        persistPendingUploadsLocked()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "[Push] Failed to upload queued clipboard item from $reason", e)
                    return
                }
            }
        }
    }

    private suspend fun pushClipboardToCloud(text: String) {
        val endpoint = currentEndpoint()
        val url = endpoint.address
        val user = currentUsernameForProfile(endpoint.profileKey)
        val pass = currentPasswordForProfile(endpoint.profileKey)
        val backend = endpoint.backend

        if (url.isBlank()) {
            throw IOException("Server address is blank")
        }

        if (backend == ServerBackend.CLIPCASCADE) {
            val activeClient = clipCascadeClient
            if (activeClient?.isConnected() == true) {
                val outgoing = SyncClient.buildClipCascadeClipboardData(this@MainService, text)
                activeClient.sendClipboard(
                    payload = outgoing.payload,
                    type = outgoing.type,
                    filename = outgoing.filename
                )
            } else {
                SyncClient.putClipboard(this@MainService, url, user, pass, backend, text)
            }
        } else {
            SyncClient.putClipboard(this@MainService, url, user, pass, backend, text)
        }
        lastUploadedContent = text
        persistLastSyncedContent(text)
        markBackendActivity()
    }

    private fun forceUploadClipboard(content: String, origin: String) {
        if (content.isBlank()) return
        val normalizedContent = OutgoingClipboardFilter.transform(this, connection?.remoteService, content)
        scope.launch {
            val queued = enqueuePendingUpload(normalizedContent)
            if (!queued) {
                return@launch
            }
            Log.d(TAG, "[Push] Received explicit upload request from $origin")
            flushPendingUploads("explicit:$origin", force = true)
        }
    }

    private enum class RuntimeMode {
        NORMAL,
        SCREEN_OFF,
        POWER_SAVE,
        AGGRESSIVE
    }

    private data class ReceiveFilter(
        val blockedExtensions: Set<String>,
        val minFileSizeBytes: Long?,
        val maxFileSizeBytes: Long?,
        val minTextChars: Int?,
        val maxTextChars: Int?
    ) {
        fun normalized(): ReceiveFilter {
            val normalizedFileBounds = normalizeBounds(minFileSizeBytes, maxFileSizeBytes)
            val normalizedTextBounds = normalizeBounds(minTextChars, maxTextChars)
            return copy(
                minFileSizeBytes = normalizedFileBounds.first,
                maxFileSizeBytes = normalizedFileBounds.second,
                minTextChars = normalizedTextBounds.first,
                maxTextChars = normalizedTextBounds.second
            )
        }

        private fun normalizeBounds(minValue: Long?, maxValue: Long?): Pair<Long?, Long?> {
            return when {
                minValue == null -> null to maxValue
                maxValue == null -> minValue to null
                minValue <= maxValue -> minValue to maxValue
                else -> maxValue to minValue
            }
        }

        private fun normalizeBounds(minValue: Int?, maxValue: Int?): Pair<Int?, Int?> {
            return when {
                minValue == null -> null to maxValue
                maxValue == null -> minValue to null
                minValue <= maxValue -> minValue to maxValue
                else -> maxValue to minValue
            }
        }
    }

    private fun createScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())
}
