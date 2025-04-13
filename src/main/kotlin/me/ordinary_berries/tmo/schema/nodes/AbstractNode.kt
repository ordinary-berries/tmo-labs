package me.ordinary_berries.tmo.schema.nodes

import me.ordinary_berries.tmo.util.math.CONSUME_METRIC
import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.events.Event
import me.ordinary_berries.tmo.schema.queue.QueuePersistenceStrategy

abstract class AbstractNode(
    private val queuePersistenceStrategy: QueuePersistenceStrategy,
    protected open val metricStorage: MetricStorage,
    protected open val nextNode: Node? = null,
    protected open val previousNode: Node? = null,
    protected open val nodeName: String? = null,
) : Node {
    protected var enqueued: MutableList<Event> = mutableListOf()

    final override fun consumeEvent(event: Event) {
        enqueued.add(event)
        metricStorage.getCounter(CONSUME_METRIC).increment(this)
    }

    final override fun consumeBatchEvents(events: Collection<Event>) {
        enqueued.addAll(events)
        metricStorage.getCounter(CONSUME_METRIC).incrementBy(this, events.size)
    }

    final override fun getNext(): Node? {
        return nextNode
    }

    final override fun getPrevious(): Node? {
        return previousNode
    }

    protected abstract fun doWorkInternal()

    final override fun doWork() {
        doWorkInternal()
        enqueued = queuePersistenceStrategy.delegate(enqueued)
    }

    override fun close() {
        getEnqueuedEvents().forEach { it.drop() }
    }

    override fun getEnqueuedEvents(): List<Event> = enqueued

    protected fun getOneEnqueuedEvent(): Event? {
        val events = getEnqueuedEvents()
        var mostPrioritized: Event? = null

        events.map { event ->
            if (mostPrioritized == null || event.getPriority() < mostPrioritized.getPriority()) {
                mostPrioritized = event
            }
        }
        mostPrioritized?.let { popEvent(it) }

        return mostPrioritized
    }

    override fun getName(): String = nodeName ?: hashCode().toString()
    override fun getGroupName(): String = this.javaClass.simpleName

    override fun getNestedNodes(): List<Node> = listOf()
    override fun getNestedNodesAmount(): Int = 0

    private fun popEvent(event: Event) {
        enqueued.remove(event)
    }
}