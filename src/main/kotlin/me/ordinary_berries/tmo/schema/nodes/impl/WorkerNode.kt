package me.ordinary_berries.tmo.schema.nodes.impl

import me.ordinary_berries.tmo.util.math.DONE_EVENT_METRIC
import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.nodes.AbstractNode
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.tick.TickSupplier
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy

class WorkerNode(
    queuePersistenceStrategy: QueuePersistenceStrategy,
    override val metricStorage: MetricStorage,
    next: Node?,
    private val tickSupplier: TickSupplier,
    previous: Node? = null,
    name: String? = null,
) : AbstractNode(
    queuePersistenceStrategy = queuePersistenceStrategy,
    metricStorage = metricStorage,
    nextNode = next,
    previousNode = previous,
    nodeName = name,
) {
    private var consumedEvent: Event? = null
    private var lockedTillTick: Double = Double.MIN_VALUE

    override fun doWorkInternal() {
        val now = tickSupplier.getTicked()

        if (consumedEvent != null && lockedTillTick < now) {
            requireNotNull(consumedEvent).markDone()
            metricStorage.getCounter(DONE_EVENT_METRIC).increment(this)
            consumedEvent = null
        }

        if (consumedEvent == null) {
            getOneEnqueuedEvent()?.let { event ->
                lockedTillTick = tickSupplier.getTicked() + event.getTicksToConsume()
                consumedEvent = event
            }
        }
    }

    override fun isLocked(): Boolean {
        return consumedEvent != null
    }
}