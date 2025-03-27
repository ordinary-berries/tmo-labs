package me.ordinary_berries.tmo.schema.queue

import me.ordinary_berries.tmo.common.Identifiable
import me.ordinary_berries.tmo.schema.events.Event

interface QueuePersistenceStrategy : Identifiable {
    fun delegate(queue: MutableList<Event>): MutableList<Event>
}