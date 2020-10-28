package xyz.abc.flutter_nim.help

import android.content.Context
import android.util.Log
import com.netease.nimlib.sdk.media.record.AudioRecorder
import com.netease.nimlib.sdk.media.record.IAudioRecordCallback
import com.netease.nimlib.sdk.media.record.RecordType
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

class AudioService constructor(ctx: Context) : IAudioRecordCallback {
    private val recorder = AudioRecorder(ctx, RecordType.AAC, 3 * 60, this)
    private val audioOut = ArrayBlockingQueue<Pair<File, Long>>(1)

    fun start() {
        Log.d("AudioService", "开始录音")

        audioOut.clear()

        recorder.startRecord()
    }

    fun cancel() {
        Log.d("AudioService", "取消录音")

        recorder.completeRecord(true)
    }

    fun stop(): Pair<File, Long>? {
        Log.d("AudioService", "停止录音")

        recorder.completeRecord(false)

        return try {
            audioOut.poll(3, TimeUnit.SECONDS)
        } catch (e: Exception) {
            e.printStackTrace()

            null
        }
    }

    override fun onRecordReady() {}

    override fun onRecordStart(audioFile: File?, recordType: RecordType?) {}

    override fun onRecordSuccess(audioFile: File, audioLength: Long, recordType: RecordType?) {
        audioOut.put(Pair(audioFile, audioLength))

        Log.d("AudioService", "录音回调 $audioLength # $audioFile")
    }

    override fun onRecordFail() {}

    override fun onRecordCancel() {}

    override fun onRecordReachedMaxTime(maxTime: Int) {}
}