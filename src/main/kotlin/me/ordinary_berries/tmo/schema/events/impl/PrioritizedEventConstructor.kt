package me.ordinary_berries.tmo.schema.events.impl

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.tick.TickSupplier
import kotlin.random.Random

class PrioritizedEventConstructor(
    private val metricStorage: MetricStorage,
    private val tickSupplier: TickSupplier,
    private val fixedTicksToConsume: Double,
    vararg prioritiesToDensity: Pair<Int, Double>,
) : EventConstructor {
    private val priorityList: List<Pair<Int, Double>>

    init {
        val total = prioritiesToDensity.sumOf { it.second }
        val normalized = prioritiesToDensity.map { it.first to (it.second / total) }.sortedBy { it.second }
        priorityList = normalized
    }

    override fun createEvent(): Event {
        return PrioritizedEvent(tickSupplier, metricStorage, fixedTicksToConsume, priorityList.getPriority())
    }

    override fun batchCreateEvents(amount: Int): List<Event> {
        return (1..amount).map { createEvent() }
    }

    private fun List<Pair<Int, Double>>.getPriority(): Int {
        val rnd = Random.nextDouble()
        return priorityList.find { rnd >= it.second }?.first
            ?: priorityList.last().first
    }
}