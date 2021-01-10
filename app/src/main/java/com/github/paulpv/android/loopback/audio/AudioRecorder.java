package com.github.paulpv.android.loopback.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.github.paulpv.android.loopback.LoopbackApp;
import com.github.paulpv.android.loopback.WtcArrayBlockingQueue;

public class AudioRecorder //
                implements Runnable
{
    private static final String  TAG  = AudioRecorder.class.getSimpleName();

    @SuppressWarnings("unused")
    private static final boolean DBG  = (LoopbackApp.DBG_LEVEL >= 1);
    private static final boolean VDBG = (LoopbackApp.DBG_LEVEL >= 2);

    public interface AudioRecorderBufferListener
    {
        void onAudioRecorderBuffer();
    }

    private final int                         mAudioSource;
    private final int                         mAudioSampleRate;
    private final int                         mAudioEncodingFormat;
    private final WtcArrayBlockingQueue       mAudioBuffers;
    private final AudioBufferPool             mAudioBuffersPool;
    private final AudioRecorderBufferListener mAudioRecorderBufferListener;

    private boolean                           mIsRunning;

    /**
     * Per http://developer.android.com/reference/android/media/AudioManager.html#startBluetoothSco()
     * "The following restrictions apply on input streams:
     * <ul>
     * <li>the format must be mono</li>
     * <li>the sampling must be 8kHz</li>
     * </ul>"
     * @param audioSource One of MediaRecorder.AudioSource.* 
     * @param audioSampleRate Must be 8kHz if recording from Bluetooth headset over SCO
     * @param audioEncodingFormat
     * @param audioBuffers
     * @param audioBuffersPool
     * @param audioRecorderBufferListener
     */
    public AudioRecorder(int audioSource, //
                    int audioSampleRate, int audioEncodingFormat, //
                    WtcArrayBlockingQueue audioBuffers, AudioBufferPool audioBuffersPool, //
                    AudioRecorderBufferListener audioRecorderBufferListener)
    {
        mAudioSource = audioSource;
        mAudioSampleRate = audioSampleRate;
        mAudioEncodingFormat = audioEncodingFormat;
        mAudioBuffers = audioBuffers;
        mAudioBuffersPool = audioBuffersPool;
        mAudioRecorderBufferListener = audioRecorderBufferListener;
    }

    /**
     * Finds the smallest multiple of AudioRecord.getMinBufferSize(...) that successfully initializes an AudioRecord.
     * 
     * AudioRecord.getMinBufferSize by itself does not actually report the minimum buffer size required to successfully create an AudioRecord.
     * On several devices, creating an AudioRecord with the value returned from getMinBufferSize results in the following error:
     *  Most: "Invalid buffer size: minFrameCount 557, frameCount 256"
     *  Captivate (minBufferSize=640, created with minBufferSize*4): "Invalid buffer size: minFrameCount 1486, frameCount 1280"
     * @param audioSource One of MediaRecorder.AudioSource.*
     * @param audioSampleRate
     * @param audioChannelConfig
     * @param audioEncodingFormat
     * @param maxMultiplier
     * @return the smallest multiple of AudioRecord.getMinBufferSize(...) that successfully initializes an AudioRecord
     * @throws IllegalArgumentException
     */
    public static int findMinBufferSizeInBytes(int audioSource, //
                    int audioSampleRate, int audioChannelConfig, int audioEncodingFormat, //
                    int maxMultiplier) //
                    throws IllegalArgumentException
    {
        Log.d(TAG, "audioSource=" + audioSource);
        Log.d(TAG, "audioSampleRate=" + audioSampleRate);
        Log.d(TAG, "audioChannelConfig=" + audioChannelConfig);
        Log.d(TAG, "audioEncodingFormat=" + audioEncodingFormat);
        int minBufferSize = AudioRecord.getMinBufferSize(audioSampleRate, audioChannelConfig, audioEncodingFormat);
        if (minBufferSize == AudioRecord.ERROR_BAD_VALUE || minBufferSize == AudioRecord.ERROR)
        {
            throw new IllegalArgumentException();
        }
        Log.i(TAG, "minBufferSize=" + minBufferSize);

        for (int i = 1; i < maxMultiplier; i++)
        {
            int bufferSize = minBufferSize * i;
            Log.i(TAG, "Trying bufferSize=" + bufferSize + " (" + minBufferSize + " * " + i + ")");

            AudioRecord audioRecord = null;
            try
            {
                audioRecord =
                    new AudioRecord(audioSource, audioSampleRate, audioChannelConfig, audioEncodingFormat, bufferSize);
                Log.i(TAG, "audioRecord.getState()=" + audioRecord.getState());
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
                {
                    Log.i(TAG, "Found bufferSize=" + bufferSize + " (" + minBufferSize + " * " + i + ")");
                    return bufferSize;
                }
            }
            catch (IllegalArgumentException e)
            {
                // ignore
                Log.w(TAG, "findMinBufferSize - IllegalArgumentException", e);
            }
            finally
            {
                if (audioRecord != null)
                {
                    audioRecord.release();
                    audioRecord = null;
                }
            }
        }

        throw new IllegalArgumentException("Failed to initialize AudioRecord with bufferSize <= " + maxMultiplier
                        + " * minBufferSize=" + minBufferSize);
    }

    public void stop()
    {
        mIsRunning = false;
    }

    public void run()
    {
        AudioRecord audioRecord = null;
        Exception error = null;

        try
        {
            Log.i(TAG, "+run()");

            mIsRunning = true;

            int minBufferSizeInBytes = findMinBufferSizeInBytes(mAudioSource, //
                            mAudioSampleRate, AudioFormat.CHANNEL_IN_MONO, mAudioEncodingFormat, //
                            10);
            int minBufferSizeInShorts = (int) Math.ceil(minBufferSizeInBytes / (Short.SIZE / (double) Byte.SIZE));
            short[] audioData = new short[minBufferSizeInShorts];

            audioRecord = new AudioRecord(mAudioSource, //
                            mAudioSampleRate, AudioFormat.CHANNEL_IN_MONO, mAudioEncodingFormat, minBufferSizeInBytes);

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            audioRecord.startRecording();

            mAudioBuffers.clear();

            int shortsRead;
            AudioBuffer buffer;

            while (mIsRunning)
            {
                if (VDBG)
                {
                    Log.i(TAG, "+recordInstance.read(...)");
                }
                shortsRead = audioRecord.read(audioData, 0, minBufferSizeInShorts);
                if (VDBG)
                {
                    Log.i(TAG, "-recordInstance.read(...); shortsRead=" + shortsRead);
                }

                if (shortsRead <= 0)
                {
                    switch (shortsRead)
                    {
                        case 0:
                            Log.w(TAG, "shortsRead=0; // ending recording");
                            break;
                        case AudioRecord.ERROR:
                            Log.e(TAG, "AudioRecord.ERROR(" + shortsRead + ")");
                            break;
                        case AudioRecord.ERROR_BAD_VALUE:
                            Log.e(TAG, "AudioRecord.ERROR_BAD_VALUE(" + shortsRead + ")");
                            break;
                        case AudioRecord.ERROR_INVALID_OPERATION:
                            Log.e(TAG, "AudioRecord.ERROR_INVALID_OPERATION(" + shortsRead + ")");
                            break;
                        default:
                            Log.e(TAG, "AudioRecord.ERROR UNKNOWN(" + shortsRead + ")");
                            break;
                    }
                    break;
                }

                buffer = mAudioBuffersPool.remove(shortsRead);
                System.arraycopy(audioData, 0, buffer.getBuffer(), 0, shortsRead);
                mAudioBuffers.add(buffer);

                if (mAudioRecorderBufferListener != null)
                {
                    mAudioRecorderBufferListener.onAudioRecorderBuffer();
                }
            }

            audioRecord.stop();
        }
        catch (Exception e)
        {
            Log.e(TAG, "run()", e);
            error = e;
        }
        finally
        {
            if (audioRecord != null)
            {
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
                {
                    audioRecord.stop();
                }
                audioRecord.release();
                audioRecord = null;
            }

            Log.i(TAG, "-run()");
        }
    }
}
