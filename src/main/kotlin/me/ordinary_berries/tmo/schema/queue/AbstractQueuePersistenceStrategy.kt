package me.ordinary_berries.tmo.schema.queue

abstract class AbstractQueuePersistenceStrategy : QueuePersistenceStrategy {
    override fun getGroupName(): String = "QueuePersistence"
    override fun getName(): String = this::class.java.simpleName
}