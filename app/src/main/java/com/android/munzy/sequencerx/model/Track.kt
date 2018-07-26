package com.android.munzy.sequencerx.model

import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log

class Track(val name: String,
            val position: Int,
            val sample: String,
            val soundId: Int,
            var steps: BooleanArray = BooleanArray(16),
            var volume: Float = 0.5f ) {

    private val TAG = "Track"

    fun updateStep(index: Int) = steps.set(index, !steps[index])

    fun tryPlay(index: Long, soundPool: SoundPool) {
        if (steps[index.toInt()]) {
            soundPool.play(this.soundId, this.volume, this.volume, 1, 0 ,1.0f )
        }
    }

}