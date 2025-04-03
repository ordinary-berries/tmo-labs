package me.ordinary_berries.tmo.util

import me.ordinary_berries.tmo.BuiltSystem
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import me.ordinary_berries.tmo.schema.nodes.Node

object SystemRunner {
    fun run(eventSupplier: EventSupplier, eventConstructor: EventConstructor, forTicks: Int, builtSystem: BuiltSystem) {
        val ticker = builtSystem.ticker

        while (ticker.getTicked() < forTicks) {
            run(eventSupplier.supplyEvents(eventConstructor), builtSystem.headNode)
            ticker.tick()
        }
    }

    fun run(events: List<Event>, head: Node) {
        head.consumeBatchEvents(events)

        var current: Node? = head

        while (current != null) {
            current.doWork()
            current.getNestedNodes().forEach { nestedNode ->
                nestedNode.also {
                    it.doWork()
                    process(it)
                }
            }

            current = current.getNext()
        }
    }

    fun process(head: Node) {
        run(listOf(), head)
    }

    private fun finalize(head: Node) {
        var current: Node? = head

        while (current != null) {
            current.close()
            current.getNestedNodes().forEach { nestedNode ->
                nestedNode.also {
                    it.close()
                    finalize(it)
                }
            }

            current = current.getNext()
        }
    }
}