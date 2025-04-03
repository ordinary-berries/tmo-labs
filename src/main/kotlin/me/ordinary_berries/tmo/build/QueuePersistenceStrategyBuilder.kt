package me.ordinary_berries.tmo.build

import me.ordinary_berries.tmo.common.Builder
import me.ordinary_berries.tmo.schema.queue.impl.DropAllQueueStrategy
import me.ordinary_berries.tmo.schema.queue.impl.DropQueueOnFixedSizeStrategy
import me.ordinary_berries.tmo.schema.queue.impl.PersistAllQueueStrategy

class QueuePersistenceStrategyBuilder(
    private val context: BuilderContext,
) {
    fun dropAllQueueStrategy(init: DropAllQueueStrategyBuilder.() -> Unit = {}): DropAllQueueStrategy {
        return DropAllQueueStrategyBuilder(context).apply(init).build()
    }

    fun persistAllQueueStrategy(init: PersistAllQueueStrategyBuilder.() -> Unit = {}): PersistAllQueueStrategy {
        return PersistAllQueueStrategyBuilder(context).apply(init).build()
    }

    fun dropQueueOnFixedSizeStrategy(init: DropQueueOnFixedSizeStrategyBuilder.() -> Unit = {}): DropQueueOnFixedSizeStrategy {
        return DropQueueOnFixedSizeStrategyBuilder(context).apply(init).build()
    }
}

class DropAllQueueStrategyBuilder(
    private val context: BuilderContext,
) : Builder<DropAllQueueStrategy> {
    override fun build(): DropAllQueueStrategy = DropAllQueueStrategy(
        metricStorage = context.metricStorage
    )

    override fun getContext(): BuilderContext = context
}

class PersistAllQueueStrategyBuilder(
    private val context: BuilderContext,
) : Builder<PersistAllQueueStrategy> {
    override fun build(): PersistAllQueueStrategy = PersistAllQueueStrategy(
        metricStorage = context.metricStorage
    )

    override fun getContext(): BuilderContext = context
}

class DropQueueOnFixedSizeStrategyBuilder(
    private val context: BuilderContext,
) : Builder<DropQueueOnFixedSizeStrategy> {
    var maximumQueueSize = 0

    override fun build(): DropQueueOnFixedSizeStrategy = DropQueueOnFixedSizeStrategy(
        context.metricStorage,
        maximumQueueSize,
    )

    override fun getContext(): BuilderContext {
        return context
    }
}
