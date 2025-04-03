package me.ordinary_berries.tmo.schema.events.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.tick.TickSupplier
import me.ordinary_berries.tmo.util.math.TIME_IN_SYSTEM_METRIC

class SimpleEvent(
    private val tickSupplier: TickSupplier,
    private val metricStorage: MetricStorage,
    private val ticksToConsume: Double
) : Event {
    private val createdAt = tickSupplier.getTicked()

    override fun getTicksToConsume(): Double {
        return ticksToConsume
    }

    override fun markDone() {
        metricStorage.getCounter(TIME_IN_SYSTEM_METRIC).incrementBy(this, tickSupplier.getTicked() - getCreatedAt())
    }

    override fun drop() {
        metricStorage.getCounter(TIME_IN_SYSTEM_METRIC).incrementBy(this, tickSupplier.getTicked() - getCreatedAt())
    }

    override fun getCreatedAt(): Int {
        return createdAt
    }

    override fun getGroupName(): String {
        return this::class.java.simpleName
    }

    override fun getName(): String {
        return hashCode().toString()
    }
}