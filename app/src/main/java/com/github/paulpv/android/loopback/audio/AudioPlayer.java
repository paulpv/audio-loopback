package com.github.paulpv.android.loopback.audio;

import android.media.AudioTrack;
import android.util.Log;

import com.github.paulpv.android.loopback.LoopbackApp;
import com.github.paulpv.android.loopback.WtcArrayBlockingQueue;

@SuppressWarnings("JavaDoc")
public class AudioPlayer implements Runnable
{
    private static final String  TAG  = AudioPlayer.class.getSimpleName();

    @SuppressWarnings("unused")
    private static final boolean DBG  = (LoopbackApp.DBG_LEVEL >= 1);
    private static final boolean VDBG = (LoopbackApp.DBG_LEVEL >= 2);

    public interface AudioPlayerBufferListener
    {
        void onAudioPlayerBuffer();
    }

    private final int                       mAudioStreamType;
    private final int                       mAudioSampleRate;
    private final int                       mAudioChannelConfig;
    private final int                       mAudioEncodingFormat;
    private final WtcArrayBlockingQueue     mAudioBuffers;
    private final AudioBufferPool           mAudioBuffersPool;
    private final AudioPlayerBufferListener mAudioPlayerBufferListener;

    private boolean                         mIsRunning;

    /**
     * Per http://developer.android.com/reference/android/media/AudioManager.html#startBluetoothSco()
     * "The following restrictions apply on output streams:
     * <ul>
     * <li>the stream type must be STREAM_VOICE_CALL</li>
     * <li>the format must be mono</li>
     * <li>the sampling must be 8kHz</li>
     * </ul>" 
     * @param audioStreamType One of AudioManager.STREAM_*; Must be AudioManager.STREAM_VOICE_CALL if playing to Bluetooth headset over SCO  
     * @param audioSampleRate Must be 8kHz or 16kHz if playing to Bluetooth headset over SCO
     * @param audioChannelConfig Must be AudioFormat.CHANNEL_OUT_MONO if playing to Bluetooth headset over SCO
     * @param audioEncodingFormat
     * @param audioBuffers
     * @param audioBuffersPool
     * @param audioPlayerBufferListener
     */
    public AudioPlayer(int audioStreamType, //
                    int audioSampleRate, int audioChannelConfig, int audioEncodingFormat, //
                    WtcArrayBlockingQueue audioBuffers, AudioBufferPool audioBuffersPool, //
                    AudioPlayerBufferListener audioPlayerBufferListener)
    {
        this.mAudioStreamType = audioStreamType;
        mAudioSampleRate = audioSampleRate;
        mAudioChannelConfig = audioChannelConfig;
        mAudioEncodingFormat = audioEncodingFormat;
        mAudioBuffers = audioBuffers;
        mAudioBuffersPool = audioBuffersPool;
        mAudioPlayerBufferListener = audioPlayerBufferListener;
    }

    /**
     * Finds the smallest multiple of AudioTrack.getMinBufferSize(...) that successfully initializes an AudioTrack.
     * 
     * AudioTrack.getMinBufferSize by itself does not actually report the minimum buffer size required to successfully create an AudioTrack.
     * On several devices, creating an AudioTrack with the value returned from getMinBufferSize results in the following error:
     *  Most: "Invalid buffer size: minFrameCount 557, frameCount 256"
     *  Captivate (minBufferSize=640, created with minBufferSize*4): "Invalid buffer size: minFrameCount 1486, frameCount 1280"
     *  
     * @param audioStreamType
     * @param audioSampleRate
     * @param audioChannelConfig
     * @param audioEncodingFormat
     * @param maxMultiplier
     * @return the smallest multiple of AudioTrack.getMinBufferSize(...) that successfully initializes an AudioTrack
     * @throws IllegalArgumentException
     */
    public static int findMinBufferSizeInBytes(int audioStreamType, //
                    int audioSampleRate, int audioChannelConfig, int audioEncodingFormat, //
                    int maxMultiplier) //
                    throws IllegalArgumentException
    {
        Log.d(TAG, "audioStreamType=" + audioStreamType);
        Log.d(TAG, "audioSampleRate=" + audioSampleRate);
        Log.d(TAG, "audioChannelConfig=" + audioChannelConfig);
        Log.d(TAG, "audioEncodingFormat=" + audioEncodingFormat);
        int minBufferSize = AudioTrack.getMinBufferSize(audioSampleRate, audioChannelConfig, audioEncodingFormat);
        if (minBufferSize == AudioTrack.ERROR_BAD_VALUE || minBufferSize == AudioTrack.ERROR)
        {
            throw new IllegalArgumentException("getMinBufferSize(...)");
        }
        Log.i(TAG, "minBufferSize=" + minBufferSize);

        for (int i = 1; i < maxMultiplier; i++)
        {
            int bufferSize = minBufferSize * i;
            Log.d(TAG, "Trying bufferSize=" + bufferSize);

            AudioTrack audioTrack = null;
            try
            {
                audioTrack =
                    new AudioTrack(audioStreamType, audioSampleRate, audioChannelConfig, audioEncodingFormat, bufferSize,
                                    AudioTrack.MODE_STREAM);
                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
                {
                    Log.i(TAG, "Found bufferSize=" + bufferSize);
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
                if (audioTrack != null)
                {
                    audioTrack.release();
                    audioTrack = null;
                }
            }
        }

        throw new IllegalArgumentException("exceeded maximum multiple of minBufferSize");
    }

    public void stop()
    {
        mIsRunning = false;
    }

    public void run()
    {
        AudioTrack audioTrack = null;
        Exception error = null;

        try
        {
            Log.i(TAG, "+run()");

            mIsRunning = true;

            int minBufferSizeInBytes = findMinBufferSizeInBytes(mAudioStreamType, //
                            mAudioSampleRate, mAudioChannelConfig, mAudioEncodingFormat, //
                            10);

            Log.i(TAG, "audioTrack = new AudioTrack(...)");
            audioTrack = new AudioTrack(mAudioStreamType, //
                            mAudioSampleRate, mAudioChannelConfig, mAudioEncodingFormat, //
                            minBufferSizeInBytes, //
                            AudioTrack.MODE_STREAM);

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            /*
            boolean speakerPhonePreference = true;//WavePreference.getSpeakerPhone(context);
            if (speakerPhonePreference)
            {
                audioManager2.setMode(AudioManager.MODE_IN_CALL);
            }
            else
            {
                audioManager2.setMode(AudioManager.MODE_NORMAL);
            }
            audioManager2.setSpeakerphoneOn(speakerPhonePreference);
            //audioManager2.setBluetoothScoOn(on);
            */

            audioTrack.play();

            AudioBuffer buffer;

            while (mIsRunning)
            {
                buffer = (AudioBuffer) mAudioBuffers.poll(0);
                if (buffer == null)
                {
                    Log.w(TAG, "buffer=null; // ending recording");
                    break;
                }

                if (VDBG)
                {
                    Log.i(TAG, "+audioTrack.write(...)");
                }
                audioTrack.write(buffer.getBuffer(), 0, buffer.getLength());
                if (VDBG)
                {
                    Log.i(TAG, "-audioTrack.write; buffer.getLength()=" + buffer.getLength());
                }

                // Return the buffer back to the pool for future re-use
                mAudioBuffersPool.add(buffer);

                if (mAudioPlayerBufferListener != null)
                {
                    mAudioPlayerBufferListener.onAudioPlayerBuffer();
                }
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "run: InterruptedException; ignoring", e);
        }
        catch (Exception e)
        {
            Log.e(TAG, "run()", e);
            error = e;
        }
        finally
        {
            if (audioTrack != null)
            {
                try
                {
                    audioTrack.flush();
                    if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED)
                    {
                        audioTrack.stop();
                    }
                    audioTrack.release();
                    audioTrack = null;
                }
                catch (Exception e)
                {
                    Log.e(TAG, "track.flush()/stop()/release()", e);
                }
            }

            Log.i(TAG, "-run()");
        }
    }
}
