package me.ordinary_berries.tmo.schema.events

interface EventConstructor {
    fun createEvent(): Event

    fun batchCreateEvents(amount: Int): List<Event>
}
