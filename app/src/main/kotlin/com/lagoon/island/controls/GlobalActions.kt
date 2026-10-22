package com.lagoon.island.controls

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Screenshot / lock / recents are only possible through an accessibility service
 * (`performGlobalAction`). The Enhanced-mode service sets [delegate] while it is connected and
 * clears it in `onUnbind`; the deck shows the three tiles only while [available] is true.
 */
@Singleton
class GlobalActions @Inject constructor() {

    interface Delegate {
        fun screenshot()
        fun lock()
        fun recents()
    }

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available

    var delegate: Delegate? = null
        set(value) {
            field = value
            _available.value = value != null
        }

    fun screenshot() = delegate?.screenshot()
    fun lock() = delegate?.lock()
    fun recents() = delegate?.recents()
}
