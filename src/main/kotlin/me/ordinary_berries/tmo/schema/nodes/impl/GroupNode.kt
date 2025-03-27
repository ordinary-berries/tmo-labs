package me.ordinary_berries.tmo.schema.nodes.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.nodes.AbstractNode
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy
import me.ordinary_berries.tmo.util.math.ALL_CHANNELS_ARE_FREE_METRIC
import me.ordinary_berries.tmo.util.math.ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC
import me.ordinary_berries.tmo.util.RoundRobinIterator

class GroupNode(
    queuePersistenceStrategy: QueuePersistenceStrategy,
    metricStorage: MetricStorage,
    private val subNodes: List<Node>,
    nextNode: Node? = null,
    previousNode: Node? = null,
    name: String? = null,
) : AbstractNode(
    queuePersistenceStrategy = queuePersistenceStrategy,
    metricStorage = metricStorage,
    nextNode = nextNode,
    previousNode = previousNode,
    nodeName = name,
) {
    private val roundRobinIterator = RoundRobinIterator(subNodes)

    override fun isLocked(): Boolean {
        return subNodes.all { it.isLocked() }
    }

    override fun doWorkInternal() {
        writeMetrics()

        while (!isLocked() && enqueued.isNotEmpty()) {
            val node = roundRobinIterator.next()
            if (!node.isLocked()) {
                node.consumeEvent(enqueued.removeFirst())
            }
        }
    }

    private fun writeMetrics() {
        if (!subNodes.any { it.isLocked() }) {
            metricStorage.getCounter(ALL_CHANNELS_ARE_FREE_METRIC).increment(this)
        }
        if (isLocked() && enqueued.isNotEmpty()) {
            metricStorage.getCounter(ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC).increment(this)
        }
    }

    override fun getNestedNodes(): List<Node> = subNodes
    override fun getNestedNodesAmount(): Int = subNodes.size
}