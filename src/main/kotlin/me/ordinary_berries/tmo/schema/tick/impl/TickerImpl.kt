package me.ordinary_berries.tmo.schema.tick.impl

import me.ordinary_berries.tmo.schema.tick.Ticker

class TickerImpl(
    private var counter: Int = 0
) : Ticker {
    override fun tick() {
        counter += 1
    }

    override fun getTicked(): Int {
        return counter
    }
}