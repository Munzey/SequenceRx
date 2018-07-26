package com.android.munzy.sequencerx.utils

enum class Note(val denominator: Int) {
    QUARTER_NOTE(1),
    SIXTEENTH_NOTE(4),
}

object BpmConverter {

    val minuteInMs = 60000

    fun bpmToDelayTime(bpm: Int, note: Note): Long {
        return ((minuteInMs / bpm) / note.denominator).toLong()
    }
}