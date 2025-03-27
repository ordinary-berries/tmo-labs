package me.ordinary_berries.tmo.schema.nodes

import me.ordinary_berries.tmo.common.Identifiable
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.common.CanWork

interface Node : CanWork, Identifiable {
    fun consumeEvent(event: Event)

    fun consumeBatchEvents(events: Collection<Event>)

    fun getEnqueuedEvents(): List<Event>

    fun isLocked(): Boolean

    fun getNext(): Node?

    fun getPrevious(): Node?

    fun getNestedNodesAmount(): Int

    fun getNestedNodes(): List<Node>
}