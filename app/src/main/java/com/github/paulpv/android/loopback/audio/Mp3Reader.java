package com.github.paulpv.android.loopback.loopback.audio;



//
// Uses the JLayer library:
//  http://www.javazoom.net/javalayer/sources.html
//  http://www.javazoom.net/javalayer/docs/docs1.0/index.html
//
// TODO:(pv) Get this to work native:
//  http://www.badlogicgames.com/wordpress/?p=446
//  http://stackoverflow.com/questions/11495578/prebuilt-ffmpeg-library-for-android
//  http://stackoverflow.com/questions/4725773/ffmpeg-on-android
//  http://stackoverflow.com/questions/6228008/decoding-audio-via-android-using-ffmpeg
//  http://stackoverflow.com/questions/11220792/android-playing-mp3-files-with-audiotrack-using-ffmpeg
//  http://www.java2s.com/Open-Source/Android/Sound-Audio-Voice/smuuz/com/projects/sebastian/Smuuz/Backend/Playback.java.htm
//  http://code.google.com/p/android-fplayer/source/browse/#svn%2Ftrunk
//  http://code.google.com/p/ringdroid/source/browse/
//  https://github.com/libgdx/libgdx/tree/master/extensions/gdx-audio
//  http://www.android-db.net/78628/Android-encoder-in-Mp3-stereo.html
//
import android.media.*;
import android.util.*;
import com.github.paulpv.android.loopback.loopback.*;
import java.io.*;
import javazoom.jl.decoder.*;

public class Mp3Reader //
                implements Runnable
{
    private static final String  TAG  = AudioRecorder.class.getSimpleName();

    @SuppressWarnings("unused")
    private static final boolean DBG  = (LoopbackApp.DBG_LEVEL >= 1);
    @SuppressWarnings("unused")
    private static final boolean VDBG = (LoopbackApp.DBG_LEVEL >= 2);

    public interface AudioRecorderBufferListener
    {
        void onAudioRecorderBuffer();
    }
	
	//private final Context                     mContext;
    private final String                      mFilePath;
    private final int                         mAudioSampleRate;
    private final int                         mAudioChannelConfig;
    private final int                         mAudioEncodingFormat;
    private final WtcArrayBlockingQueue       mAudioBuffers;
    private final AudioBufferPool             mAudioBuffersPool;
    private final AudioRecorderBufferListener mAudioRecorderBufferListener;

    private boolean                           mIsRecording;

    public Mp3Reader(//Context context, //
	                String filePath, //
                    WtcArrayBlockingQueue audioBuffers, AudioBufferPool audioBuffersPool, //
                    AudioRecorderBufferListener audioRecorderBufferListener) //
                    throws FileNotFoundException
    {
		//mContext = context;
        mFilePath = filePath;

        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        InputStream inputStream = new BufferedInputStream(fileInputStream, 8 * 1024);
        Bitstream bitstream = new Bitstream(inputStream);
        Header frameHeader = null;
        try
        {
            frameHeader = bitstream.readFrame();
        }
        catch (BitstreamException e)
        {
            throw new IllegalArgumentException("Cannot read first frame from MP3");
        }

        Decoder decoder = new Decoder();
        SampleBuffer output = null;
        try
        {
            output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
        }
        catch (DecoderException e)
        {
            throw new IllegalArgumentException("Cannot decode first frame from MP3");
        }

        switch (output.getChannelCount())
        {
            case 2:
                mAudioChannelConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            case 1:
                mAudioChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
                break;
            default:
                throw new IllegalArgumentException("MP3 must be Stereo or Mono");
        }

        mAudioSampleRate = output.getSampleFrequency();
        mAudioEncodingFormat = AudioFormat.ENCODING_PCM_16BIT;

        mAudioBuffers = audioBuffers;
        mAudioBuffersPool = audioBuffersPool;
        mAudioRecorderBufferListener = audioRecorderBufferListener;
    }

    public int getAudioSampleRate()
    {
        return mAudioSampleRate;
    }

    public int getAudioChannelConfig()
    {
        return mAudioChannelConfig;
    }

    public int getAudioEncodingFormat()
    {
        return mAudioEncodingFormat;
    }

    public void stop()
    {
        mIsRecording = false;
    }

    public void run()
    {
        Bitstream bitstream = null;
        Exception error = null;
        long numFrames = 0;
        long timeStart = System.currentTimeMillis();

        try
        {
            Log.i(TAG, "+run()");

            mIsRecording = true;

            // When a debugger/USB is attached JLayer is slower-than-real-time.
            // Because of that, don't set priority to real-time audio.
            // TODO:(pv) Get a native real-time MP3 decoder working (ffmeg/mpeg123/etc)...
            //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            File file = new File(mFilePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStream inputStream = new BufferedInputStream(fileInputStream, 8 * 1024);
            bitstream = new Bitstream(inputStream);

            Decoder decoder = new Decoder();

            mAudioBuffers.clear();

            Header frameHeader;
            SampleBuffer audioDecoded;
            int lengthDecoded;
            AudioBuffer audioBuffer;

            while (mIsRecording)
            {
                frameHeader = bitstream.readFrame();

                if (frameHeader == null)
                {
                    mIsRecording = false;
                    break;
                }

                audioDecoded = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);

                numFrames++;

                lengthDecoded = audioDecoded.getBufferLength();

                // BUG:(pv) The app can run out of memory on moderately sized audio files
                // TODO:(pv) Find somewhere better than the heap to buffer these?
                // Alternatively, just trap low memory condition and pause this thread?
                audioBuffer = mAudioBuffersPool.remove(lengthDecoded);
                System.arraycopy(audioDecoded.getBuffer(), 0, audioBuffer.getBuffer(), 0, lengthDecoded);

                mAudioBuffers.add(audioBuffer);

                if (mAudioRecorderBufferListener != null)
                {
                    mAudioRecorderBufferListener.onAudioRecorderBuffer();
                }

                bitstream.closeFrame();
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "run()", e);
            error = e;
        }
        finally
        {
            if (bitstream != null)
            {
                try
                {
                    bitstream.close();
                }
                catch (BitstreamException e)
                {
                    // ignore
                }
                bitstream = null;
            }

            long timeStop = System.currentTimeMillis();
            long timeElapsed = timeStop - timeStart;
            Log.i(TAG, "-run(); numFrames=" + numFrames + ", timeElapsed=" + timeElapsed);
        }
    }
}
