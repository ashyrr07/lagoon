package com.lagoon.island.sources.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.lagoon.island.R
import com.lagoon.island.data.AppsRepository
import com.lagoon.island.data.GestureAction
import com.lagoon.island.data.SettingsRepository
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import com.lagoon.island.service.IslandNotificationListener
import com.lagoon.island.sources.IslandSource
import com.lagoon.island.sources.NotificationBus
import com.lagoon.island.util.IntentLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Mirrors the priority media session as one `media:<package>` activity.
 *
 * Follows the system's active-session list while the notification listener is bound (that
 * binding is what grants `getActiveSessions`), prefers a playing session over an idle one at
 * index 0, and re-posts on every metadata / playback-state callback. Album art is decoded off the
 * main thread and patched in afterwards. A stopped session lingers [MediaRules.STOPPED_GRACE_MS]
 * and is then removed; paused sessions stay until the app releases them.
 *
 * Action ids: see [MediaRules] (`prev`, `play_pause`, `next`, and `seek` with the millisecond
 * position as text). [command] serves `IslandEngine.mediaCommand`.
 */
@Singleton
class MediaSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val registry: ActivityRegistry,
    private val bus: NotificationBus,
    private val settingsRepository: SettingsRepository,
    private val launcher: IntentLauncher,
    private val apps: AppsRepository,
    private val artLoader: AlbumArtLoader,
) : IslandSource {

    private val manager: MediaSessionManager? by lazy { context.getSystemService(MediaSessionManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())
    private var scope: CoroutineScope? = null
    private var listening = false

    private var controller: MediaController? = null
    private var callback: MediaController.Callback? = null
    private var activityId: String? = null
    private var startedAt = 0L
    private var updatedAt = 0L
    private var trackKey: String? = null
    private var art: Bitmap? = null
    private var artKey: String? = null
    private var artJob: Job? = null
    private var stopJob: Job? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { onSessions(it.orEmpty()) }

    override fun start(scope: CoroutineScope) {
        this.scope = scope
        scope.launch {
            combine(
                bus.listener.map { it != null },
                settingsRepository.settings.map { it.features.media }.distinctUntilChanged(),
            ) { bound, enabled -> bound && enabled }
                .distinctUntilChanged()
                .collect { if (it) attach() else detach() }
        }
    }

    override fun stop() {
        detach()
        scope = null
    }

    /** Engine hook: `engine.mediaCommand = mediaSource::command`. Ignores activities we do not own. */
    fun command(activity: IslandActivity, action: GestureAction) {
        val c = controller?.takeIf { MediaRules.activityId(it.packageName) == activity.id } ?: return
        when (action) {
            GestureAction.PlayPause -> togglePlay(c)
            GestureAction.NextTrack -> c.transportControls.skipToNext()
            GestureAction.PreviousTrack -> c.transportControls.skipToPrevious()
            else -> Unit
        }
    }

    // ---- session following -------------------------------------------------------------------

    private fun attach() {
        if (listening) return
        val manager = manager ?: return
        val component = IslandNotificationListener.componentName(context)
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            listening = true
            onSessions(manager.getActiveSessions(component))
        } catch (e: SecurityException) {
            Log.w(TAG, "No media session access yet", e)
        }
    }

    private fun detach() {
        if (listening) {
            runCatching { manager?.removeOnActiveSessionsChangedListener(sessionsListener) }
            listening = false
        }
        follow(null)
    }

    private fun refreshSessions() {
        if (!listening) return
        val manager = manager ?: return
        runCatching { manager.getActiveSessions(IslandNotificationListener.componentName(context)) }
            .onSuccess { onSessions(it) }
    }

    private fun onSessions(sessions: List<MediaController>) {
        follow(MediaRules.pickPrioritySession(sessions) { MediaRules.isPlaying(it.playbackState?.state) })
    }

    private fun follow(next: MediaController?) {
        val current = controller
        if (current != null && next != null && current.sessionToken == next.sessionToken) return
        current?.let { c -> callback?.let { runCatching { c.unregisterCallback(it) } } }
        callback = null
        stopJob?.cancel(); stopJob = null
        artJob?.cancel(); artJob = null
        val previousId = activityId
        controller = next
        activityId = next?.let { MediaRules.activityId(it.packageName) }
        if (previousId != null && previousId != activityId) registry.remove(previousId)
        if (next == null) {
            activityId = null
            return
        }
        if (previousId != activityId) {
            startedAt = System.currentTimeMillis()
            updatedAt = startedAt
            trackKey = null
            art = null
            artKey = null
        }
        val cb = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
            override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
            override fun onSessionDestroyed() {
                if (controller === next) {
                    follow(null)
                    refreshSessions()
                }
            }
        }
        next.registerCallback(cb, handler)
        callback = cb
        publish()
    }

    // ---- publishing ----------------------------------------------------------------------------

    private fun publish() {
        val c = controller ?: return
        val id = activityId ?: return
        val metadata = c.metadata
        val state = c.playbackState
        val stateCode = state?.state
        val packageName = c.packageName
        val appName = apps.labelOf(packageName)

        val title = metadata?.text(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.text(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = metadata?.text(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.text(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: metadata?.text(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val album = metadata?.text(MediaMetadata.METADATA_KEY_ALBUM)

        // A freshly created session with no metadata and nothing playing is not worth a pill yet.
        if (title == null && !MediaRules.isPlaying(stateCode)) {
            if (MediaRules.isStopped(stateCode)) scheduleRemoval(id)
            return
        }

        val now = System.currentTimeMillis()
        val key = "$title|$artist|$album"
        if (key != trackKey) {
            trackKey = key
            updatedAt = now // a new track resurfaces a dismissed media pill
        }
        loadArt(id, metadata)

        if (MediaRules.isStopped(stateCode)) scheduleRemoval(id) else { stopJob?.cancel(); stopJob = null }

        val playing = MediaRules.isPlaying(stateCode)
        val sampledAt = state?.lastPositionUpdateTime?.takeIf { it > 0 }
            ?.let { MediaRules.elapsedToWallClock(it, SystemClock.elapsedRealtime(), now) } ?: now
        val content = ActivityContent.Media(
            title = title ?: appName,
            subtitle = artist ?: album ?: "",
            appName = appName,
            packageName = packageName,
            art = art,
            isPlaying = playing,
            positionMs = (state?.position ?: 0L).coerceAtLeast(0L),
            durationMs = (metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L).coerceAtLeast(0L),
            positionSampledAt = sampledAt,
            speed = state?.playbackSpeed?.takeIf { playing } ?: 1f,
        )
        registry.post(
            IslandActivity(
                id = id,
                source = Source.Media,
                priority = Priority.Media,
                content = content,
                startedAt = startedAt,
                updatedAt = updatedAt,
                actions = actions(c, playing),
                onTap = { open(c) },
            ),
        )
    }

    private fun actions(c: MediaController, playing: Boolean): List<IslandAction> {
        val transport = c.transportControls
        return listOf(
            IslandAction(MediaRules.ACTION_PREV, context.getString(R.string.island_action_previous)) { transport.skipToPrevious() },
            IslandAction(MediaRules.ACTION_PLAY_PAUSE, context.getString(if (playing) R.string.island_action_pause else R.string.island_action_play)) { togglePlay(c) },
            IslandAction(MediaRules.ACTION_NEXT, context.getString(R.string.island_action_next)) { transport.skipToNext() },
            IslandAction(MediaRules.ACTION_SEEK, context.getString(R.string.island_action_seek), acceptsText = true) { text ->
                MediaRules.parseSeekMillis(text)?.let { transport.seekTo(it) }
            },
        )
    }

    private fun togglePlay(c: MediaController) {
        if (MediaRules.isPlaying(c.playbackState?.state)) c.transportControls.pause() else c.transportControls.play()
    }

    private fun open(c: MediaController) {
        val sessionActivity = c.sessionActivity
        if (sessionActivity != null && launcher.send(sessionActivity)) return
        apps.launchIntent(c.packageName)?.let { launcher.launch(it) }
    }

    private fun scheduleRemoval(id: String) {
        if (stopJob?.isActive == true) return
        stopJob = scope?.launch {
            delay(MediaRules.STOPPED_GRACE_MS)
            if (activityId == id) registry.remove(id)
        }
    }

    private fun loadArt(id: String, metadata: MediaMetadata?) {
        val key = artLoader.key(metadata)
        if (key == artKey) return
        artKey = key
        artJob?.cancel()
        if (key == null) {
            art = null
            return
        }
        artJob = scope?.launch {
            val bitmap = artLoader.load(metadata)
            if (artKey != key || activityId != id) return@launch
            art = bitmap
            registry.update(id) { a ->
                (a.content as? ActivityContent.Media)?.let { a.copy(content = it.copy(art = bitmap)) } ?: a
            }
        }
    }

    private fun MediaMetadata.text(key: String): String? =
        (getText(key) ?: getString(key))?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private const val TAG = "MediaSource"
    }
}
