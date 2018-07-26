package com.android.munzy.sequencerx

import android.content.res.Resources
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.android.munzy.sequencerx.model.Track
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import android.media.SoundPool


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val disposables = CompositeDisposable()

    private lateinit var buttons: Array<Button>

    private lateinit var tracks: Array<Track>

    private var selectedTrack: Int = 0

    private var delayTime: Long = 125 //125ms = 120 bpm 1/16 note

    private val soundPool: SoundPool = SoundPool.Builder().setMaxStreams(6).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttons = arrayOf(
                button1, button2, button3, button4,
                button5, button6, button7, button8,
                button9, button10, button11, button12,
                button13, button14, button15, button16
        )
    }

    override fun onStart() {
        super.onStart()

        tracks = arrayOf(
                Track("kick", 0, "kick.wav", soundPool.load(this, R.raw.kick, 1))
        )

        val buttonClickStream = createButtonClickObservable()
        val clock = clock()

        disposables.add(buttonClickStream
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map{ //Log.i(TAG, "button: $it pressed!")
                    tracks[selectedTrack].updateStep(it)
                it}
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe{
//                    Log.i(TAG, tracks[selectedTrack].steps.contentToString())
                    redrawButton(it)
                })

        tracks.forEach { track ->
            clock.subscribeOn(Schedulers.newThread())
                    .subscribe{
                        //Log.i(TAG, "firing index $it")
                        track.tryPlay(it, soundPool)
                    }
        }

        disposables.add(clock.connect())
    }

    private fun createButtonClickObservable(): Observable<Int> {

        return Observable.create { emitter ->

            buttons.forEach { button ->
                button.setOnClickListener {

                    emitter.onNext(buttons.indexOf(it))
                }
            }

            emitter.setCancellable {
                buttons.forEach { button ->
                    button.setOnClickListener(null)
                }
            }
        }
    }

    private fun clock(): ConnectableObservable<Long> {
        return Observable.interval(delayTime, TimeUnit.MILLISECONDS)
                //.filter { tick -> tick % delayTime == 0L}
                .scan(-1, { acc: Long, step: Long ->
                    //Log.i(TAG,"acc is: $acc")
                    if (acc >= 15L) 0 else acc + 1
                }).skip(1).publish()
    }

    private fun redrawButton(index: Int) {
        if (tracks[selectedTrack].steps[index]) {
            buttons[index].backgroundTintList = this.getResources().getColorStateList(R.color.material_blue_grey_950)
        } else {
            buttons[index].backgroundTintList = this.getResources().getColorStateList(R.color.material_grey_300)
        }
    }

    @Override
    override fun onStop() {
        super.onStop()
        soundPool.release()
        if (!disposables.isDisposed) {
            disposables.dispose()
        }
    }
}
