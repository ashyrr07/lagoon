package com.lagoon.island.ui.app.demo

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagoon.island.R
import com.lagoon.island.engine.ActivityContent
import com.lagoon.island.engine.ActivityRegistry
import com.lagoon.island.engine.IconSource
import com.lagoon.island.engine.IslandAction
import com.lagoon.island.engine.IslandActivity
import com.lagoon.island.engine.Priority
import com.lagoon.island.engine.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Posts realistic fake activities so the island can be tried, screenshotted and QA'd without real
 * sources. Each call takes the screen's [Resources], so labels and sample texts follow the app's
 * language (song titles and names stay as they are).
 */
@HiltViewModel
class DemoViewModel @Inject constructor(
    private val registry: ActivityRegistry,
) : ViewModel() {

    private var progressJob: Job? = null
    private var mediaJob: Job? = null

    private val art: Bitmap by lazy { gradientBitmap(0xFF7F5AF0.toInt(), 0xFF2CB67D.toInt(), 0xFFFF8906.toInt()) }
    private val avatar: Bitmap by lazy { gradientBitmap(0xFF25D366.toInt(), 0xFF128C7E.toInt(), 0xFF075E54.toInt(), letter = "A") }

    fun music(res: Resources) {
        val now = System.currentTimeMillis()
        var playing = true
        var position = 42_000L
        fun post() {
            registry.post(
                IslandActivity(
                    id = ID_MEDIA,
                    source = Source.Demo,
                    priority = Priority.Media,
                    content = ActivityContent.Media(
                        title = "Midnight City",
                        subtitle = "M83",
                        appName = "Demo Player",
                        packageName = "com.lagoon.island",
                        art = art,
                        isPlaying = playing,
                        positionMs = position,
                        durationMs = 243_000L,
                        positionSampledAt = System.currentTimeMillis(),
                    ),
                    startedAt = now,
                    updatedAt = System.currentTimeMillis(),
                    actions = listOf(
                        IslandAction("prev", res.getString(R.string.island_action_previous)) { position = 0L; post() },
                        IslandAction("play_pause", res.getString(if (playing) R.string.island_action_pause else R.string.island_action_play)) {
                            val t = System.currentTimeMillis()
                            position = (registry[ID_MEDIA]?.content as? ActivityContent.Media)?.let { m ->
                                if (m.isPlaying) m.positionMs + (t - m.positionSampledAt) else m.positionMs
                            } ?: position
                            playing = !playing
                            post()
                        },
                        IslandAction("next", res.getString(R.string.island_action_next)) { position = 0L; post() },
                        IslandAction("seek", res.getString(R.string.island_action_seek)) { text -> text?.toLongOrNull()?.let { position = it; post() } },
                    ),
                ),
            )
        }
        post()
        mediaJob?.cancel()
        mediaJob = viewModelScope.launch {
            // Keep the demo honest: when the track ends, start over.
            while (isActive) {
                delay(5_000)
                val m = registry[ID_MEDIA]?.content as? ActivityContent.Media ?: break
                val t = System.currentTimeMillis()
                val pos = if (m.isPlaying) m.positionMs + (t - m.positionSampledAt) else m.positionMs
                if (pos >= m.durationMs) { position = 0L; post() }
            }
        }
    }

    fun incomingCall(res: Resources) {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_CALL,
                source = Source.Demo,
                priority = Priority.Alert,
                content = ActivityContent.Call("Ada Lovelace", res.getString(R.string.island_demo_call_detail), avatar, ActivityContent.Call.State.Ringing, now),
                startedAt = now,
                alertMs = 30_000L,
                ttlMs = 30_000L,
                actions = listOf(
                    IslandAction("answer", res.getString(R.string.island_call_answer)) { activeCall(res) },
                    IslandAction("decline", res.getString(R.string.island_call_decline)) { registry.remove(ID_CALL) },
                ),
            ),
        )
    }

    fun activeCall(res: Resources) {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_CALL,
                source = Source.Demo,
                priority = Priority.Call,
                content = ActivityContent.Call("Ada Lovelace", res.getString(R.string.island_demo_call_detail), avatar, ActivityContent.Call.State.Active, now),
                startedAt = now,
                ttlMs = 60_000L,
                actions = listOf(IslandAction("hang_up", res.getString(R.string.island_call_hang_up)) { registry.remove(ID_CALL) }),
            ),
        )
    }

    fun timer(res: Resources) {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_TIMER,
                source = Source.Demo,
                priority = Priority.Timer,
                content = ActivityContent.Timer(res.getString(R.string.island_demo_timer_label), endAt = now + 5 * 60_000L, baseAt = now, running = true),
                startedAt = now,
                ttlMs = 5 * 60_000L + 2_000L,
                actions = listOf(
                    IslandAction("add", "+1:00") {
                        registry.update(ID_TIMER) { a ->
                            val t = a.content as ActivityContent.Timer
                            a.copy(content = t.copy(endAt = (t.endAt ?: now) + 60_000L), updatedAt = System.currentTimeMillis())
                        }
                    },
                    IslandAction("cancel", res.getString(R.string.island_demo_cancel)) { registry.remove(ID_TIMER) },
                ),
            ),
        )
    }

    fun charging() {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_CHARGING,
                source = Source.Demo,
                priority = Priority.Alert,
                content = ActivityContent.Charging(level = 82, watts = 27f, state = ActivityContent.Charging.State.Connected),
                startedAt = now,
                alertMs = 3_000L,
                ttlMs = 8_000L,
            ),
        )
    }

    fun earbuds() {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_BLUETOOTH,
                source = Source.Demo,
                priority = Priority.Alert,
                content = ActivityContent.Bluetooth("Pixel Buds Pro", true, battery = 80, left = 80, right = 76, case = 54, kind = ActivityContent.Bluetooth.Kind.Earbuds),
                startedAt = now,
                alertMs = 3_000L,
                ttlMs = 8_000L,
            ),
        )
    }

    fun message(res: Resources) {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_MESSAGE,
                source = Source.Demo,
                priority = Priority.Notification,
                content = ActivityContent.Notification(
                    key = "demo:whatsapp",
                    packageName = "com.whatsapp",
                    appName = "WhatsApp",
                    appIcon = IconSource.Res(R.drawable.ic_launcher_foreground),
                    title = "Ada · Weekend dinner group",
                    text = res.getString(R.string.island_demo_message_text),
                    largeIcon = IconSource.Image(avatar),
                    isConversation = true,
                ),
                startedAt = now,
                ttlMs = 60_000L,
                actions = listOf(
                    IslandAction("reply", res.getString(R.string.island_reply), acceptsText = true) { text -> Log.i(TAG, "Demo reply: $text") },
                    IslandAction("mark_read", res.getString(R.string.island_demo_mark_read)) { registry.remove(ID_MESSAGE) },
                ),
            ),
        )
    }

    fun navigation(res: Resources) {
        val now = System.currentTimeMillis()
        registry.post(
            IslandActivity(
                id = ID_NAV,
                source = Source.Demo,
                priority = Priority.Navigation,
                content = ActivityContent.Navigation(
                    res.getString(R.string.island_demo_nav_instruction),
                    res.getString(R.string.island_demo_nav_distance),
                    res.getString(R.string.island_demo_nav_eta),
                    icon = null,
                ),
                startedAt = now,
                ttlMs = 120_000L,
            ),
        )
    }

    fun progress(res: Resources) {
        val now = System.currentTimeMillis()
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            var p = 0f
            while (isActive && p <= 1f) {
                registry.post(
                    IslandActivity(
                        id = ID_PROGRESS,
                        source = Source.Demo,
                        priority = Priority.Progress,
                        content = ActivityContent.Progress(
                            res.getString(R.string.island_demo_progress_title),
                            ((12 * (1f - p)).toInt() + 1).let { res.getQuantityString(R.plurals.island_demo_progress_eta, it, it) },
                            p,
                            icon = null,
                        ),
                        startedAt = now,
                        updatedAt = System.currentTimeMillis(),
                        ttlMs = 30_000L,
                    ),
                )
                delay(1_000)
                p += 0.05f
            }
            registry.remove(ID_PROGRESS)
        }
    }

    fun clearAll() {
        progressJob?.cancel()
        mediaJob?.cancel()
        registry.removeWhere { it.source == Source.Demo }
    }

    /** Leaving the Try it screen takes every demo activity off the island, so none get stuck. */
    override fun onCleared() {
        clearAll()
    }

    companion object {
        private const val TAG = "LagoonDemo"
        const val ID_MEDIA = "demo:media"
        const val ID_CALL = "demo:call"
        const val ID_TIMER = "demo:timer"
        const val ID_CHARGING = "demo:charging"
        const val ID_BLUETOOTH = "demo:bluetooth"
        const val ID_MESSAGE = "demo:message"
        const val ID_NAV = "demo:navigation"
        const val ID_PROGRESS = "demo:progress"

        /** A square gradient bitmap, optionally with a letter in the middle (fake art / avatar). */
        fun gradientBitmap(vararg colors: Int, size: Int = 256, letter: String? = null): Bitmap {
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), colors, null, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
            if (letter != null) {
                val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    textSize = size * 0.5f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                val y = size / 2f - (text.descent() + text.ascent()) / 2f
                canvas.drawText(letter, size / 2f, y, text)
            }
            return bitmap
        }
    }
}
