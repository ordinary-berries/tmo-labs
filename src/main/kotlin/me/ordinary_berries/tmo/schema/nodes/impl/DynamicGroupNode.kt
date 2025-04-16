package me.ordinary_berries.tmo.schema.nodes.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.nodes.AbstractNode
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.nodes.NodeConstructor
import me.ordinary_berries.tmo.schema.nodes.NodeSupplier
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy
import me.ordinary_berries.tmo.util.math.ALL_CHANNELS_ARE_FREE_METRIC
import me.ordinary_berries.tmo.util.math.ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.DYNAMIC_GROUP_AGENTS_AMOUNT_METRIC
import me.ordinary_berries.tmo.util.math.poissonDistribution

class DynamicGroupNode(
    private val minSize: Int,
    private val maxSize: Int,
    private val shrinkDensity: Double,
    private val nodeConstructor: NodeConstructor,
    private val nodeSupplier: NodeSupplier,
    queuePersistenceStrategy: QueuePersistenceStrategy,
    override val metricStorage: MetricStorage,
    override val nextNode: Node?,
    override val previousNode: Node?,
) : AbstractNode(
    queuePersistenceStrategy = queuePersistenceStrategy,
    metricStorage = metricStorage,
    nextNode = nextNode,
    previousNode = previousNode,
) {
    private val subNodes: MutableList<Node> = mutableListOf()
    private var stoppingNodes: MutableList<Node> = mutableListOf()

    init {
        while (subNodes.size < minSize) {
            subNodes.add(nodeConstructor.createNode())
        }
    }

    private var idx: Int = 0

    override fun doWorkInternal() {
        while (!isLocked() && enqueued.isNotEmpty()) {
            val node = getNextNodeRoundRobin()
            if (!node.isLocked()) {
                val event = requireNotNull(getOneEnqueuedEvent())
                node.consumeEvent(event)
            }
        }

        writeMetrics()
        tryAddSubNodes()
        tryShrinkSubNodes()
    }

    override fun isLocked(): Boolean {
        return subNodes.all { it.isLocked() }
    }

    override fun getNestedNodes(): List<Node> = subNodes + stoppingNodes
    override fun getNestedNodesAmount(): Int = subNodes.size + stoppingNodes.size

    private fun writeMetrics() {
        if (!subNodes.any { it.isLocked() }) {
            metricStorage.getCounter(ALL_CHANNELS_ARE_FREE_METRIC).increment(this)
        }
        if (isLocked() && enqueued.isNotEmpty()) {
            metricStorage.getCounter(ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC).increment(this)
        }

        metricStorage.getCounter(DYNAMIC_GROUP_AGENTS_AMOUNT_METRIC).incrementBy(this, getNestedNodesAmount())
    }

    private fun tryAddSubNodes() {
        val newNodes = nodeSupplier.supplyNodes(nodeConstructor)
        subNodes.addAll(newNodes.take(maxSize - subNodes.size))
    }

    private fun tryShrinkSubNodes() {
        var k = poissonDistribution(shrinkDensity)
        while (k > 0 && subNodes.size > minSize) {
            stoppingNodes.add(subNodes.removeFirst())
            k--
        }

        val workingNodes = mutableListOf<Node>()
        stoppingNodes.map { node ->
            if (node.isLocked()) {
                workingNodes.add(node)
            }
        }

        stoppingNodes = workingNodes
    }

    private fun getNextNodeRoundRobin(): Node {
        if (idx >= subNodes.size) {
            idx = 0
        }

        return subNodes[idx++]
    }
}