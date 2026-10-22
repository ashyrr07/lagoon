package com.lagoon.island.sources

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fan-out of the notification listener's callbacks so that several sources can consume them
 * without each needing a reference to the service.
 */
@Singleton
class NotificationBus @Inject constructor() {
    sealed interface Event {
        data class Posted(val sbn: StatusBarNotification) : Event
        data class Removed(val sbn: StatusBarNotification, val reason: Int) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 128)
    val events: SharedFlow<Event> = _events

    /** The bound listener, or null while disconnected. Use for getActiveNotifications / cancel. */
    private val _listener = MutableStateFlow<NotificationListenerService?>(null)
    val listener: StateFlow<NotificationListenerService?> = _listener

    fun onConnected(service: NotificationListenerService) {
        _listener.value = service
    }

    fun onDisconnected() {
        _listener.value = null
    }

    fun emit(event: Event) {
        _events.tryEmit(event)
    }

    /** Everything currently in the shade, or empty while disconnected. */
    fun activeNotifications(): List<StatusBarNotification> =
        runCatching { _listener.value?.activeNotifications?.toList() }.getOrNull().orEmpty()

    fun cancel(key: String) {
        runCatching { _listener.value?.cancelNotification(key) }
    }
}
