package me.ordinary_berries.tmo.schema.events

interface EventSupplier {
    fun supplyEvents(eventConstructor: EventConstructor): List<Event>
}