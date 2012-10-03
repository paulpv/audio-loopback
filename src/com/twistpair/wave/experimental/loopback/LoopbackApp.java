package com.twistpair.wave.experimental.loopback;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.util.*;
import com.twistpair.wave.experimental.loopback.audio.*;
import com.twistpair.wave.experimental.loopback.audio.AudioPlayer.*;
import com.twistpair.wave.experimental.loopback.audio.AudioStateManager.*;
import java.io.*;
import java.util.*;
//import android.bluetooth.BluetoothDevice;

// Ideas came from PhoneApp:
// https://github.com/android/platform_packages_apps_phone/blob/master/src/com/android/phone/PhoneApp.java
public class LoopbackApp //
                extends Application //
                implements AudioStateListener, //
                AudioPlayerBufferListener, //
                AudioRecorder.AudioRecorderBufferListener, //
                Mp3Reader.AudioRecorderBufferListener //
{
    public static final String        TAG                                 = LoopbackApp.class.getSimpleName();

    /**
     * App-wide debug level:
     *   0 - no debug logging
     *   1 - normal debug logging if ro.debuggable is set (which is true in
     *       "eng" and "userdebug" builds but not "user" builds)
     *   2 - ultra-verbose debug logging
     *
     * Most individual classes in the phone app have a local DBG constant,
     * typically set to
     *   (PhoneApp.DBG_LEVEL >= 1)
     * or else
     *   (PhoneApp.DBG_LEVEL >= 2)
     * depending on the desired verbosity.
     *
     * ***** DO NOT RELEASE WITH DBG_LEVEL > 0 *****
     */
    public static int                 DBG_LEVEL                           = 1;

    @SuppressWarnings("unused")
    private static final boolean      DBG                                 = (LoopbackApp.DBG_LEVEL >= 1);
    private static final boolean      VDBG                                = (LoopbackApp.DBG_LEVEL >= 2);

    /**
     * <ul>
     * <li>msg.arg1: mAudioBuffers.size()</li>
     * <li>msg.arg2: mAudioBuffersPool.size()</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int           MSG_UPDATE_BUFFER_COUNT             = 1;

    /**
     * <ul>
     * <li>msg.arg1: unused</li>
     * <li>msg.arg2: unused</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int           MSG_UPDATE_BLUETOOTH_INDICATION     = 2;

    /**
     * <ul>
     * <li>msg.arg1: unused</li>
     * <li>msg.arg2: unused</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int           MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE = 3;

    private static LoopbackApp        sInstance;

    //AppNotificationManager       mNotificationManager;

    //private boolean                 mShowBluetoothIndication    = false;

    private final Set<Context>        mForegroundContexts                 = new HashSet<Context>();

    private final LoopbackPreferences mPreferences;

    private AudioStateManager         mAudioStateManager;

    //private static final int     EVENT_WIRED_HEADSET_PLUG = 1;
    //private static final int     EVENT_DOCK_STATE_CHANGED = 2;

    private Handler                   mHandler;

    public LoopbackApp()
    {
        sInstance = this;
        mPreferences = new LoopbackPreferences(this);
    }

    /**
     * Returns the singleton instance of the LoopbackApp.
     */
    public static LoopbackApp getInstance()
    {
        if (sInstance == null)
        {
            throw new IllegalStateException("LoopbackApp.getInstance() == null");
        }
        return sInstance;
    }

    public LoopbackPreferences getPreferences()
    {
        return mPreferences;
    }

    /**
     * AudioStateListener.getContext()
     */
    /*
    public Context getContext()
    {
        return this;
    }
    */

    /*
    public AppNotificationManager getNotificationManager()
    {
        return mNotificationManager;
    }
    */

    public AudioStateManager getAudioStateManager()
    {
        return mAudioStateManager;
    }

    @Override
    public void onCreate()
    {
        mHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                LoopbackApp.this.handleMessage(msg);
            }
        };

        //mNotificationManager = AppNotificationManager.init(this);

        mAudioStateManager = new AudioStateManager(this, this);
        mAudioStateManager.start();
    }

    @Override
    public void onTerminate()
    {
        if (mAudioStateManager != null)
        {
            mAudioStateManager.stop();
            mAudioStateManager = null;
        }

        super.onTerminate();
    }

    public void addForegroundContext(Context context)
    {
        mForegroundContexts.add(context);
    }

    public void removeForegroundContext(Context context)
    {
        mForegroundContexts.remove(context);
    }

    /**
     * Finds and returns the requested foreground activity, or null if one does not exist. 
     * @param activityClass the requested Activity class to find
     * @return the requested foreground activity, or null if one does not exist
     */
    public Context getForegroundContext(Class<?> contextClass)
    {
        for (Context context : mForegroundContexts)
        {
            if (context.getClass().equals(contextClass))
            {
                return context;
            }
        }
        return null;
    }

    public boolean isForegroundContext(Context context)
    {
        return getForegroundContext(context.getClass()) != null;
    }

    /*
    public void updateBluetoothIndication2(boolean forceUiUpdate)
    {
        //mShowBluetoothIndication = isBluetoothAudioConnected();
        if (forceUiUpdate)
        {
            // Post Handler messages to the various components that might
            // need to be refreshed based on the new state.
            MainActivity mainActivity = (MainActivity) getForegroundContext(MainActivity.class);
            if (mainActivity != null)
            {
                mainActivity.requestUpdateBluetoothIndication();
            }

            if (DBG)
            {
                Log.d(TAG, "- updating in-call notification for BT state change...");
            }
            mHandler.sendEmptyMessage(EVENT_UPDATE_INCALL_NOTIFICATION);
        }

        // Update the Proximity sensor based on Bluetooth audio state
        //updateProximitySensorMode(mCM.getState());
    }
    */

    /*
    boolean isHeadsetPlugged()
    {
        return mIsHeadsetPlugged;
    }

    public boolean isBluetoothHeadsetConnected()
    {
        return mBluetoothHeadsetState == BluetoothHeadset.STATE_CONNECTED;
    }

    public boolean isBluetoothHeadsetAudioConnected()
    {
        return mBluetoothHeadsetState == BluetoothHeadset.STATE_CONNECTED
                        && mBluetoothHeadsetAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED;
    }

    private boolean shouldTurnOnSpeaker()
    {
        return !isBluetoothHeadsetConnected() && !mIsHeadsetPlugged;
    }
    */

    public void onSpeakerphoneOn()
    {
        Log.i(TAG, "onSpeakerphoneOn()");
    }

    public void onSpeakerphoneOff()
    {
        Log.i(TAG, "onSpeakerphoneOff()");
    }

    public void onWiredHeadsetConnection(int state, String name, int microphone)
    {
        Log.i(TAG, "onWiredHeadsetConnection(state=" + state + ", name=" + name + ", microphone=" + microphone + ")");
        /*
        Log.i(TAG, "EVENT_WIRED_HEADSET_PLUG");

        // Since the presence of a wired headset or bluetooth affects the
        // speakerphone, update the "speaker" state.  We ONLY want to do
        // this on the wired headset connect / disconnect events for now
        // though, so we're only triggering on EVENT_WIRED_HEADSET_PLUG.

        if (!isBluetoothHeadsetConnected())
        {
        	if (mIsHeadsetPlugged)
        	{
        		// if the state is "connected", force the speaker off without
        		// storing the state.
        		AudioUtils.setSpeakerphoneOn(getApplicationContext(), false, false);
        	}
        	else
        	{
        		// if the state is "not connected", restore the speaker state.
        		AudioUtils.restoreSpeakerphone(getApplicationContext());
        	}
        }

        // Update the Proximity sensor based on headset state
        //updateProximitySensorMode(phoneState);
        
        */
    }

    public void onDockConnection(int state)
    {
        Log.i(TAG, "onDockConnection(" + state + ")");

        // If the phone is docked/undocked during a call, and no wired or BT headset
        // is connected: turn on/off the speaker accordingly.
        boolean inDockMode = false;
        if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED)
        {
            inDockMode = true;
        }

        if (VDBG)
        {
            Log.d(TAG, "inDockMode=" + inDockMode);
        }

        /*
        if (shouldTurnOnSpeaker())
        {
            AudioUtils.setSpeakerphoneOn(getApplicationContext(), inDockMode, true);
        }
        */
    }

    public void onBluetoothHeadsetConnected()
    {
        Log.i(TAG, "onBluetoothHeadsetConnected()");

        if (mAudioStateManager.isBluetoothScoAvailableOffCall())
        {
            mAudioStateManager.startBluetoothSco();
        }

        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();
    }

    public void onBluetoothHeadsetDisconnected()
    {
        Log.i(TAG, "onBluetoothHeadsetDisconnected()");

        //if (mAudioStateManager.isBluetoothScoOn())
        //{
        mAudioStateManager.stopBluetoothSco();
        //}

        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();
    }

    public void onBluetoothHeadsetAudioConnected()
    {
        Log.i(TAG, "onBluetoothHeadsetAudioConnected()");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();
    }

    public void onBluetoothHeadsetAudioDisconnected()
    {
        Log.i(TAG, "onBluetoothHeadsetAudioDisconnected()");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();
    }

    private int mPreviousAudioOutputAudioTrackStreamType = -1;

    public void resetPreviousAudioOutputAudioTrackStreamType()
    {
        Log.e(TAG, "Resetting mPreviousAudioOutputAudioTrackStreamType to -1");
        mPreviousAudioOutputAudioTrackStreamType = -1;
    }

    public int getPreviousAudioOutputAudioTrackStreamType()
    {
        return mPreviousAudioOutputAudioTrackStreamType;
    }

    public void onAudioManagerScoAudioConnected()
    {
        Log.i(TAG, "onAudioManagerScoAudioConnected()");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();

        if (mPreviousAudioOutputAudioTrackStreamType != -1)
        {
            Log.e(TAG, "previousAudioOutputAudioTrackStreamType already set; headset reporting");
            return;
        }

        mPreviousAudioOutputAudioTrackStreamType = mPreferences.getAudioOutputAudioTrackStreamType();
        Log.e(TAG,
                        "Set previousAudioOutputAudioTrackStreamType="
                                        + AudioStateManager.audioOutputStreamTypeToString(mPreviousAudioOutputAudioTrackStreamType));

        // TODO:(pv) If speaker is playing, close and re-open speaker

        int streamType = AudioManager.STREAM_VOICE_CALL;
        Log.e(TAG,
                        "Setting mPreferences.putAudioOutputAudioTrackStreamType("
                                        + AudioStateManager.audioOutputStreamTypeToString(streamType) + ")");
        mPreferences.putAudioOutputAudioTrackStreamType(streamType);

        mHandler //
        .obtainMessage(MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE) //
        .sendToTarget();
    }

    public void onAudioManagerScoAudioDisconnected(boolean error)
    {
        Log.i(TAG, "onAudioManagerScoAudioDisconnected()");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();

        // Force SCO off to try to repair what appears to be internal BT SCO state bugs in the OS.
        // Sometimes the only way to get SCO to connect is to reboot the phone and/or turn BT off and then back on.
        mAudioStateManager.stopBluetoothSco();

        if (mPreviousAudioOutputAudioTrackStreamType == -1)
        {
            Log.e(TAG, "Not restoring previousAudioOutputAudioTrackStreamType because it is not set");
            return;
        }

        Log.e(TAG,
                        "Restoring previousAudioOutputAudioTrackStreamType="
                                        + AudioStateManager.audioOutputStreamTypeToString(mPreviousAudioOutputAudioTrackStreamType));
        int streamType = mPreviousAudioOutputAudioTrackStreamType;
        Log.e(TAG,
                        "Setting mPreferences.putAudioOutputAudioTrackStreamType("
                                        + AudioStateManager.audioOutputStreamTypeToString(streamType) + ")");
        mPreferences.putAudioOutputAudioTrackStreamType(streamType);

        resetPreviousAudioOutputAudioTrackStreamType();

        // TODO:(pv) If speaker is playing, close and re-open speaker

        mHandler //
        .obtainMessage(MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE) //
        .sendToTarget();
    }

    public void onAudioPlayerBuffer()
    {
        if (VDBG)
        {
            Log.i(TAG, "onAudioPlayerBuffer()");
        }
        onAudioBuffer();
    }

    public void onAudioRecorderBuffer()
    {
        if (VDBG)
        {
            Log.i(TAG, "onAudioRecorderBuffer()");
        }
        onAudioBuffer();
    }

    public void onAudioBuffer()
    {
        int audioBufferCount = mAudioBuffers.size();
        int audioBufferPoolCount = mAudioBuffersPool.size();

        if (VDBG)
        {
            Log.i(TAG, "mAudioBuffers.size()=" + audioBufferCount + ", mAudioBuffersPool.size()=" + audioBufferPoolCount);
        }

        mHandler //
        .obtainMessage(MSG_UPDATE_BUFFER_COUNT, audioBufferCount, audioBufferPoolCount) //
        .sendToTarget();
    }

    private void handleMessage(Message msg)
    {
        if (DBG)
        {
            Log.i(TAG, "handleMessage(" + msg + ")");
        }

        MainActivity mainActivity = (MainActivity) getForegroundContext(MainActivity.class);
        if (mainActivity != null)
        {
            mainActivity.handleMessage(msg);
        }
    }

    // TODO:(pv) Move below code to a Service class...

    private Mp3Reader                   mMp3Reader;
    private AudioRecorder               mAudioRecorder;
    private AudioPlayer                 mAudioPlayer;

    private int                         mAudioPlayerSampleRate;
    private int                         mAudioPlayerChannelConfig;
    private int                         mAudioPlayerEncodingFormat;

    private final WtcArrayBlockingQueue mAudioBuffers     = new WtcArrayBlockingQueue("QueueAudioBuffers");
    private final AudioBufferPool       mAudioBuffersPool = new AudioBufferPool();

    public void audioStopReadingMp3()
    {
        if (mMp3Reader != null)
        {
            mMp3Reader.stop();
            mMp3Reader = null;
        }
    }

    public void audioStartReadingMp3()
    {
        String audioInputFilePath = mPreferences.getAudioInputFilePath();

        try
        {
            mMp3Reader = new Mp3Reader(audioInputFilePath, mAudioBuffers, mAudioBuffersPool, this);
        }
        catch (FileNotFoundException e)
        {
            Log.e(TAG, "EXCEPTION: new Mp3Reader(" + audioInputFilePath + ", ...)", e);
            return;
        }

        mAudioPlayerSampleRate = mMp3Reader.getAudioSampleRate();
        mAudioPlayerChannelConfig = mMp3Reader.getAudioChannelConfig();
        mAudioPlayerEncodingFormat = mMp3Reader.getAudioEncodingFormat();

        Thread thread = new Thread(mMp3Reader);
        thread.setName("Mp3Reader");
        thread.start();
    }

    public void audioStopReadingMicrophone()
    {
        if (mAudioRecorder != null)
        {
            mAudioRecorder.stop();
            mAudioRecorder = null;
        }
    }

    public void audioStartReadingMicrophone()
    {
        int audioInputAudioRecordSourceType = mPreferences.getAudioInputAudioRecordSourceType();

        mAudioPlayerSampleRate = 8000;
        mAudioPlayerChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
        mAudioPlayerEncodingFormat = AudioFormat.ENCODING_PCM_16BIT;

        mAudioRecorder = new AudioRecorder(audioInputAudioRecordSourceType, //
                        mAudioPlayerSampleRate, mAudioPlayerEncodingFormat, //
                        mAudioBuffers, mAudioBuffersPool, this);
        Thread thread = new Thread(mAudioRecorder);
        thread.setName("AudioRecorder");
        thread.start();
    }

    public void audioStopPlayingSource()
    {
        if (mAudioPlayer != null)
        {
            mAudioPlayer.stop();
            mAudioPlayer = null;
        }
    }

    public void audioStartPlayingSource()
    {
        int audioOutputAudioTrackStreamType = mPreferences.getAudioOutputAudioTrackStreamType();

        mAudioPlayer = new AudioPlayer(audioOutputAudioTrackStreamType, //
                        mAudioPlayerSampleRate, mAudioPlayerChannelConfig, mAudioPlayerEncodingFormat, //
                        mAudioBuffers, mAudioBuffersPool, this);
        Thread thread = new Thread(mAudioPlayer);
        thread.setName("AudioPlayer");
        thread.start();
    }
}
