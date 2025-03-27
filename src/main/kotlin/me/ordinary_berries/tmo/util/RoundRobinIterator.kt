package me.ordinary_berries.tmo.util

import java.util.function.Consumer

class RoundRobinIterator<T>(
    private val collection: Collection<T>,
) : Iterator<T> {
    private var innerIterator = collection.iterator()

    override fun hasNext(): Boolean = collection.isNotEmpty()

    override fun next(): T {
        if (collection.isEmpty()) {
            throw NoSuchElementException("Collection is empty")
        }

        if (!innerIterator.hasNext()) {
            innerIterator = collection.iterator()
        }

        return innerIterator.next()
    }

    override fun forEachRemaining(action: Consumer<in T>) {
        while (innerIterator.hasNext()) {
            action.accept(innerIterator.next())
        }

        innerIterator = collection.iterator()
    }
}