package me.ordinary_berries.tmo.schema.nodes.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.nodes.NodeConstructor
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy
import me.ordinary_berries.tmo.schema.tick.TickSupplier

class WorkerNodeConstructor(
    private val queuePersistenceStrategy: QueuePersistenceStrategy,
    private val metricStorage: MetricStorage,
    private val tickSupplier: TickSupplier,
) : NodeConstructor {
    override fun createNode(): Node {
        return WorkerNode(
            queuePersistenceStrategy = queuePersistenceStrategy,
            metricStorage = metricStorage,
            tickSupplier = tickSupplier,
            next = null,
        )
    }

    override fun batchCreateNodes(amount: Int): List<Node> {
        return (1..amount).map { createNode() }
    }
}