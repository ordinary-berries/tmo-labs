package me.ordinary_berries.tmo.build

import me.ordinary_berries.tmo.BuiltSystem
import me.ordinary_berries.tmo.common.Builder
import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.tick.impl.TickerImpl

class SystemBuilder(
    tickerImpl: TickerImpl? = null,
    metricStorage: MetricStorage? = null,
) : Builder<BuiltSystem> {
    var builderContext: BuilderContext = BuilderContext(tickerImpl ?: TickerImpl(), metricStorage)
    var headNode: ConcreteNodeBuilder<*>? = null

    val nodeBuilder: NodeBuilder = NodeBuilder(builderContext)
    val queueBuilder: QueuePersistenceStrategyBuilder = QueuePersistenceStrategyBuilder(builderContext)

    override fun build(): BuiltSystem {
        val notNullHeadNode = requireNotNull(headNode).build()

        return BuiltSystem(
            headNode = notNullHeadNode,
            metricStorage = builderContext.metricStorage,
            ticker = builderContext.ticker,
        )
    }

    override fun getContext(): BuilderContext {
        return builderContext
    }
}

fun system(tickerImpl: TickerImpl? = null, metricStorage: MetricStorage? = null, init: SystemBuilder.() -> Unit): BuiltSystem {
    return SystemBuilder(tickerImpl, metricStorage).apply(init).build()
}
