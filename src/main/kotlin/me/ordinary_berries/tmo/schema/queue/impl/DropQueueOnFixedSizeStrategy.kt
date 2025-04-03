package me.ordinary_berries.tmo.schema.queue.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.queue.AbstractQueuePersistenceStrategy
import me.ordinary_berries.tmo.util.math.DROP_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.EVENT_PERSIST_METRIC

class DropQueueOnFixedSizeStrategy(
    private val metricStorage: MetricStorage,
    private val maximumQueueSize: Int,
) : AbstractQueuePersistenceStrategy() {
    override fun delegate(queue: MutableList<Event>): MutableList<Event> {
        val newQueue = if (queue.size > maximumQueueSize) {
            metricStorage.getCounter(DROP_EVENT_METRIC).incrementBy(this, queue.size - maximumQueueSize)
            queue.forEachIndexed { index, event -> if (index < queue.size - maximumQueueSize) event.drop() }
            queue.takeLast(maximumQueueSize).toMutableList()
        } else {
            queue
        }
        metricStorage.getCounter(EVENT_PERSIST_METRIC).incrementBy(this, newQueue.size)

        return newQueue
    }
}