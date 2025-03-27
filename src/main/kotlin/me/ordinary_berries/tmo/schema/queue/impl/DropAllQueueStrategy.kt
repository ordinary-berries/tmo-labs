package me.ordinary_berries.tmo.schema.queue.impl

import me.ordinary_berries.tmo.util.math.DROP_EVENT_METRIC
import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.queue.AbstractQueuePersistenceStrategy

class DropAllQueueStrategy(
    private val metricStorage: MetricStorage
) : AbstractQueuePersistenceStrategy() {
    override fun delegate(queue: MutableList<Event>): MutableList<Event> {
        metricStorage.getCounter(DROP_EVENT_METRIC).incrementBy(this, queue.size)
        return mutableListOf()
    }
}