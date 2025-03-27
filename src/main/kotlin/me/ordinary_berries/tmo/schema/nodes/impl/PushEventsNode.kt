package me.ordinary_berries.tmo.schema.nodes.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.nodes.AbstractNode
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy
import me.ordinary_berries.tmo.util.math.PUSH_EVENT_METRIC

class PushEventsNode(
    queuePersistenceStrategy: QueuePersistenceStrategy,
    metricStorage: MetricStorage,
    override val nextNode: Node,
    previous: Node? = null,
    name: String? = null,
) : AbstractNode(
    queuePersistenceStrategy = queuePersistenceStrategy,
    metricStorage = metricStorage,
    nextNode = nextNode,
    previousNode = previous,
    nodeName = name,
) {
    override fun doWorkInternal() {
        while (!nextNode.isLocked()) {
            val event = getOneEnqueuedEvent()
            if (event == null) break

            nextNode.consumeEvent(event)
            metricStorage.getCounter(PUSH_EVENT_METRIC).increment(this)
        }
    }

    override fun isLocked(): Boolean = nextNode.isLocked()
}