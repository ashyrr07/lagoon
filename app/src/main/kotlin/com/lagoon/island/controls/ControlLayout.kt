package com.lagoon.island.controls

/**
 * Pure helpers that turn the saved id lists in [com.lagoon.island.data.ControlSettings] into what
 * the deck and the Controls screen show. No Android types, unit-tested.
 */
object ControlLayout {

    /**
     * The ids to show, in the user's order: saved ids that still exist (and are available),
     * duplicates dropped. Ids the user never saw (new in this version) are NOT appended; the
     * Controls screen lists them under "Available" instead, so an update never changes the deck.
     */
    fun resolve(saved: List<String>, available: Collection<String>): List<String> {
        val allowed = available.toSet()
        return saved.filter { it in allowed }.distinct()
    }

    /** Ids the user could add: available but not chosen, in the registry's order. */
    fun remaining(saved: List<String>, available: List<String>): List<String> {
        val chosen = saved.toSet()
        return available.filter { it !in chosen }
    }

    /** Move the item at [from] to [to]; out-of-range indices leave the list untouched. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }

    /** Append [id] if absent, otherwise remove it. */
    fun <T> toggle(list: List<T>, id: T): List<T> =
        if (id in list) list - id else list + id

    /** Remove [id] wherever it is. */
    fun <T> remove(list: List<T>, id: T): List<T> = list.filter { it != id }

    /** Split tiles into grid rows of [columns]. */
    fun <T> rows(items: List<T>, columns: Int): List<List<T>> =
        if (columns <= 0) emptyList() else items.chunked(columns)
}
