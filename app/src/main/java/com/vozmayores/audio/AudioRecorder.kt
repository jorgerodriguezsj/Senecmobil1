package com.vozmayores.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "Voz.AudioRecorder"

class AudioRecorder {
    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var record: AudioRecord? = null
    private var readerJob: Job? = null
    private val chunks = mutableListOf<ShortArray>()
    private var totalSamples = 0

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (record != null) {
            Log.w(TAG, "start() while already recording, ignored")
            return true
        }
        val minBufBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufBytes <= 0) {
            Log.e(TAG, "getMinBufferSize returned $minBufBytes")
            return false
        }
        // Cushion contra under-runs. Un factor 4x da ~250 ms de holgura a 16 kHz.
        val bufBytes = minBufBytes * 4
        val ar = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build(),
                )
                .setBufferSizeInBytes(bufBytes)
                .build()
        } catch (t: Throwable) {
            Log.e(TAG, "AudioRecord.Builder threw", t)
            return false
        }
        if (ar.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized (state=${ar.state})")
            ar.release()
            return false
        }
        chunks.clear()
        totalSamples = 0
        record = ar
        ar.startRecording()

        val chunkSize = bufBytes / 2 // bytes → shorts
        readerJob = scope.launch {
            val buf = ShortArray(chunkSize)
            while (isActive) {
                val n = ar.read(buf, 0, chunkSize)
                if (n > 0) {
                    synchronized(chunks) {
                        chunks.add(buf.copyOf(n))
                        totalSamples += n
                    }
                } else if (n < 0) {
                    Log.e(TAG, "AudioRecord.read error $n")
                    break
                }
            }
        }
        Log.d(TAG, "start ok bufBytes=$bufBytes minBufBytes=$minBufBytes")
        return true
    }

    suspend fun stop(): ShortArray {
        readerJob?.cancelAndJoin()
        readerJob = null
        record?.let { ar ->
            try {
                ar.stop()
            } catch (t: Throwable) {
                Log.w(TAG, "stop() on AudioRecord threw", t)
            }
            ar.release()
        }
        record = null
        val out: ShortArray
        synchronized(chunks) {
            out = ShortArray(totalSamples)
            var off = 0
            for (c in chunks) {
                c.copyInto(out, off)
                off += c.size
            }
            chunks.clear()
            totalSamples = 0
        }
        Log.d(TAG, "stop samples=${out.size} duration=${out.size * 1000L / SAMPLE_RATE_HZ}ms")
        return out
    }
}
