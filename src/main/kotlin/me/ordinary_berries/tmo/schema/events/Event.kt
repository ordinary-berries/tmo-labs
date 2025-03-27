package me.ordinary_berries.tmo.schema.events

import me.ordinary_berries.tmo.common.Identifiable

interface Event : Identifiable {
    fun getTicksToConsume(): Double

    fun markDone()

    fun getCreatedAt(): Int
}