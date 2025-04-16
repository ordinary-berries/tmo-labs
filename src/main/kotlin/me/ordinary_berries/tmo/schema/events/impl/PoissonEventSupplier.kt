package me.ordinary_berries.tmo.schema.events.impl

import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import me.ordinary_berries.tmo.util.math.poissonDistribution

class PoissonEventSupplier(
    private val lambda: Double,
) : EventSupplier {
    override fun supplyEvents(eventConstructor: EventConstructor): List<Event> {
        return eventConstructor.batchCreateEvents(poissonDistribution(lambda))
    }
}