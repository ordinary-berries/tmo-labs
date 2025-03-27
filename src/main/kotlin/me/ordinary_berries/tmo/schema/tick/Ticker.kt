package me.ordinary_berries.tmo.schema.tick

interface Ticker : TickSupplier {
    fun tick()
}