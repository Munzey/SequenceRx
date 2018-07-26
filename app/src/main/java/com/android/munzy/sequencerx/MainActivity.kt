package com.android.munzy.sequencerx

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.android.munzy.sequencerx.model.Track
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit
import android.media.SoundPool
import android.util.Log
import android.view.inputmethod.EditorInfo
import com.android.munzy.sequencerx.utils.BpmConverter
import com.android.munzy.sequencerx.utils.Note
import io.reactivex.observables.ConnectableObservable


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val disposables = CompositeDisposable()

    private lateinit var stepButtons: Array<Button>

    private lateinit var trackButtons: Array<Button>

    private lateinit var tracks: Array<Track>

    private var selectedTrack: Int = 0

    private var soundPool: SoundPool = SoundPool.Builder().setMaxStreams(10).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        stepButtons = arrayOf(
                button1, button2, button3, button4,
                button5, button6, button7, button8,
                button9, button10, button11, button12,
                button13, button14, button15, button16
        )

        trackButtons = arrayOf(
                trackAButton, trackBButton, trackCButton
        )
    }

    override fun onStart() {
        super.onStart()

        tracks = arrayOf(
                Track("kick", soundPool.load(this, R.raw.kick, 1)),
                Track("snare", soundPool.load(this, R.raw.snare, 1)),
                Track("hihat", soundPool.load(this, R.raw.hihat, 1))
        )

        val stepButtonClickStream = createButtonClickObservable(stepButtons)
        val trackButtonClickStream = createButtonClickObservable(trackButtons)
        val tempoChangeStream = createTextEditObservable()

        disposables.add(stepButtonClickStream
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

        disposables.add(trackButtonClickStream
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .distinctUntilChanged()
                .map{
                    if (it != selectedTrack) {
                        selectedTrack = it
                        true
                    } else {
                        false
                    }
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe{
                    if (it) redrawAllButtons(selectedTrack)
                }
        )

        val tempoClock: ConnectableObservable<Long> = tempoChangeStream
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .map{
                    BpmConverter.bpmToDelayTime(it, Note.SIXTEENTH_NOTE)
                }
                .switchMap {
                    clock(it)
                }.replay()

        tracks.forEach { track ->
            disposables.add(tempoClock.observeOn(Schedulers.newThread())
                    .subscribe{
                        track.tryPlay(it, soundPool)
                    })
        }

        disposables.add(tempoClock.connect())
    }

    private fun createButtonClickObservable(buttons: Array<Button>): Observable<Int> {

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

    private fun createTextEditObservable(): Observable<Int> {
        return Observable.create { emitter ->
            editText.setOnEditorActionListener { textView, id, keyEvent ->
                if (id == EditorInfo.IME_ACTION_DONE) {
                    emitter.onNext(textView.text.toString().toInt())
                }
                false // let system handle rest
            }

            emitter.setCancellable {
                editText.setOnEditorActionListener(null)
            }
        }
    }

    private fun clock(delayTime: Long): Observable<Long> {
        return Observable.interval(delayTime, TimeUnit.MILLISECONDS)
                //.filter { tick -> tick % delayTime == 0L}
                .scan(-1, { acc: Long, step: Long ->
                    //Log.i(TAG,"acc is: $acc")
                    if (acc >= 15L) 0 else acc + 1
                }).skip(1)
    }

    private fun redrawButton(stepIndex: Int) {
        if (tracks[selectedTrack].steps[stepIndex]) {
            stepButtons[stepIndex].backgroundTintList = this.getResources().getColorStateList(R.color.material_blue_grey_950)
        } else {
            stepButtons[stepIndex].backgroundTintList = this.getResources().getColorStateList(R.color.material_grey_300)
        }
    }

    private fun redrawAllButtons(trackIndex: Int) {
        tracks[selectedTrack].steps.forEachIndexed { index, b ->
            redrawButton(index)
        }
    }

    @Override
    override fun onStop() {
        super.onStop()
        soundPool.release()
        // TODO should also probably set soundPool to null
        if (!disposables.isDisposed) {
            disposables.dispose()
        }
    }
}
