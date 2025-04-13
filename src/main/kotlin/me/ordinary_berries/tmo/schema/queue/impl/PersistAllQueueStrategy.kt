package me.ordinary_berries.tmo.schema.queue.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.queue.AbstractQueuePersistenceStrategy
import me.ordinary_berries.tmo.util.math.getPrioritizedEventPersistMetricName

class PersistAllQueueStrategy(
    private val metricStorage: MetricStorage,
) : AbstractQueuePersistenceStrategy() {
    override fun delegate(queue: MutableList<Event>): MutableList<Event> {
        queue.map { event ->
            metricStorage.getCounter(getPrioritizedEventPersistMetricName(event.getPriority())).increment(this)
        }
        return queue
    }
}