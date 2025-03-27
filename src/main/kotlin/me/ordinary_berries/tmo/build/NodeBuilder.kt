package me.ordinary_berries.tmo.build

import me.ordinary_berries.tmo.common.Builder
import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.nodes.impl.GroupNode
import me.ordinary_berries.tmo.schema.nodes.impl.PushEventsNode
import me.ordinary_berries.tmo.schema.nodes.impl.WorkerNode
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy
import me.ordinary_berries.tmo.schema.tick.TickSupplier

sealed interface ConcreteNodeBuilder<T: Node> : Builder<T>

class NodeBuilder(
    private val context: BuilderContext,
) {
    fun groupNode(subNodes: List<ConcreteNodeBuilder<*>>, init: GroupNodeBuilder.() -> Unit = {}): GroupNodeBuilder {
        return GroupNodeBuilder(context, QueuePersistenceStrategyBuilder(context), subNodes.toMutableList()).apply(init)
    }

    fun pushEventsNode(nextNode: ConcreteNodeBuilder<*>, init: PushEventsNodeBuilder.() -> Unit = {}): PushEventsNodeBuilder {
        return PushEventsNodeBuilder(context, QueuePersistenceStrategyBuilder(context), nextNode).apply(init)
    }

    fun workerNode(init: WorkerNodeBuilder.() -> Unit = {}): WorkerNodeBuilder {
        return WorkerNodeBuilder(context, QueuePersistenceStrategyBuilder(context)).apply(init)
    }
}

class GroupNodeBuilder(
    private val context: BuilderContext,
    private val queuePersistenceStrategyBuilder: QueuePersistenceStrategyBuilder,
    var subNodes: MutableList<ConcreteNodeBuilder<*>> = mutableListOf()
) : ConcreteNodeBuilder<GroupNode> {
    var metricStorage: MetricStorage = context.metricStorage
    var queuePersistenceStrategy: QueuePersistenceStrategy = queuePersistenceStrategyBuilder.dropAllQueueStrategy()
    var nextNode: Node? = null
    var previousNode: Node? = null
    var name: String? = null

    override fun getContext(): BuilderContext {
        return context
    }

    override fun build(): GroupNode =
        GroupNode(
            queuePersistenceStrategy = queuePersistenceStrategy,
            metricStorage = metricStorage,
            subNodes = subNodes.map { it.build() },
            nextNode = nextNode,
            previousNode = previousNode,
            name = name,
        )
}

class PushEventsNodeBuilder(
    private val context: BuilderContext,
    private val queuePersistenceStrategyBuilder: QueuePersistenceStrategyBuilder,
    var nextNode: ConcreteNodeBuilder<*>,
) : ConcreteNodeBuilder<PushEventsNode> {
    var metricStorage: MetricStorage = context.metricStorage
    var queuePersistenceStrategy: QueuePersistenceStrategy = queuePersistenceStrategyBuilder.dropAllQueueStrategy()
    var previousNode: Node? = null
    var name: String? = null

    override fun build(): PushEventsNode =
        PushEventsNode(
            queuePersistenceStrategy = queuePersistenceStrategy,
            metricStorage = metricStorage,
            nextNode = nextNode.build(),
            previous = previousNode,
            name = name,
        )

    override fun getContext(): BuilderContext {
        return context
    }
}

class WorkerNodeBuilder(
    private val context: BuilderContext,
    private val queuePersistenceStrategyBuilder: QueuePersistenceStrategyBuilder,
) : ConcreteNodeBuilder<WorkerNode> {
    var metricStorage: MetricStorage = context.metricStorage
    var queuePersistenceStrategy: QueuePersistenceStrategy = queuePersistenceStrategyBuilder.dropAllQueueStrategy()
    var nextNode: ConcreteNodeBuilder<*>? = null
    var previousNode: ConcreteNodeBuilder<*>? = null
    var tickSupplier: TickSupplier = context.ticker
    var name: String? = null

    override fun build(): WorkerNode =
        WorkerNode(
            queuePersistenceStrategy = queuePersistenceStrategy,
            metricStorage = metricStorage,
            next = nextNode?.build(),
            previous = previousNode?.build(),
            tickSupplier = tickSupplier,
            name = name,
        )

    override fun getContext(): BuilderContext {
        return context
    }
}
