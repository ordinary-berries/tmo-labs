package me.ordinary_berries.tmo.schema.events.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.tick.TickSupplier

class SimpleEventConstructor(
    private val metricStorage: MetricStorage,
    private val tickSupplier: TickSupplier,
    private val fixedTicksToConsume: Double
) : EventConstructor {
    override fun createEvent(): Event {
        return SimpleEvent(tickSupplier, metricStorage, fixedTicksToConsume)
    }

    override fun batchCreateEvents(amount: Int): List<Event> {
        if (amount < 1) {
            return emptyList()
        }

        return (1..amount).map { createEvent() }
    }
}