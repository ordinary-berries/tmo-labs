package me.ordinary_berries.tmo.schema.events.impl

import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import kotlin.math.exp
import kotlin.random.Random

class PoissonEventSupplier(
    private val lambda: Double,
) : EventSupplier {
    override fun supplyEvents(eventConstructor: EventConstructor): List<Event> {
        val l = exp(-lambda)
        var k = 0
        var p = 1.0

        while (true) {
            p *= Random.nextDouble()
            if (p > l) {
                k++
            } else {
                return eventConstructor.batchCreateEvents(k)
            }
        }
    }
}