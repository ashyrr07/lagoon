package com.lagoon.island.engine

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** The live set of activities, keyed by id. Sources post, update and remove; the engine observes. */
@Singleton
class ActivityRegistry @Inject constructor() {
    private val _activities = MutableStateFlow<Map<String, IslandActivity>>(emptyMap())
    val activities: StateFlow<Map<String, IslandActivity>> = _activities

    fun post(activity: IslandActivity) {
        _activities.update { it + (activity.id to activity) }
    }

    fun update(id: String, transform: (IslandActivity) -> IslandActivity) {
        _activities.update { map -> map[id]?.let { map + (id to transform(it)) } ?: map }
    }

    fun remove(id: String) {
        _activities.update { it - id }
    }

    fun removeWhere(predicate: (IslandActivity) -> Boolean) {
        _activities.update { map -> map.filterValues { !predicate(it) } }
    }

    fun clear() {
        _activities.value = emptyMap()
    }

    operator fun get(id: String): IslandActivity? = _activities.value[id]
}
