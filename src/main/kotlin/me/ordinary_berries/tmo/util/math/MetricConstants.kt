package me.ordinary_berries.tmo.util.math

const val CONSUME_METRIC = "consume"
const val DROP_EVENT_METRIC = "drop"
const val EVENT_PERSIST_METRIC = "event-persist"
const val PUSH_EVENT_METRIC = "push"
const val DONE_EVENT_METRIC = "done"
const val TIME_IN_SYSTEM_METRIC = "time-in-system"
const val ALL_CHANNELS_ARE_FREE_METRIC = "all-channels-are-free"
const val ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC = "all-channels-are-locked-and-have-new-event"

const val TIME_IN_SYSTEM_METRIC_PRIORITIZED = "$TIME_IN_SYSTEM_METRIC-prioritized"

fun getPrioritizedTimeInSystemMetricName(priority: Int): String {
    return "${TIME_IN_SYSTEM_METRIC_PRIORITIZED}_$priority"
}

fun getPrioritizedEventPersistMetricName(priority: Int): String {
    return "${EVENT_PERSIST_METRIC}_$priority"
}
