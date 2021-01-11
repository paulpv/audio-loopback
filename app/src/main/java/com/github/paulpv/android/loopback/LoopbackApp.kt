package com.github.paulpv.android.loopback

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.github.paulpv.android.loopback.audio.*
import com.github.paulpv.android.loopback.audio.AudioPlayer.AudioPlayerBufferListener
import com.github.paulpv.android.loopback.audio.AudioStateManager.AudioStateListener
import java.io.FileNotFoundException
import java.util.*

//import android.bluetooth.BluetoothDevice;
// Ideas came from PhoneApp:
// https://github.com/android/platform_packages_apps_phone/blob/master/src/com/android/phone/PhoneApp.java
class LoopbackApp //
//
    : Application(), AudioStateListener, AudioPlayerBufferListener, AudioRecorder.AudioRecorderBufferListener, Mp3Reader.AudioRecorderBufferListener //
{
    //AppNotificationManager       mNotificationManager;
    //private boolean                 mShowBluetoothIndication    = false;
    private val mForegroundContexts: MutableSet<Context> = HashSet()
    lateinit var preferences: LoopbackPreferences

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

    lateinit var audioStateManager: AudioStateManager
        private set

    //private static final int     EVENT_WIRED_HEADSET_PLUG = 1;
    //private static final int     EVENT_DOCK_STATE_CHANGED = 2;
    private lateinit var mHandler: Handler

    override fun onCreate() {
        super.onCreate()

        preferences = LoopbackPreferences(this)

        mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                this@LoopbackApp.handleMessage(msg)
            }
        }

        //mNotificationManager = AppNotificationManager.init(this);
        audioStateManager = AudioStateManager(this, this)
        audioStateManager.start()
    }

    override fun onTerminate() {
        audioStateManager.stop()
        super.onTerminate()
    }

    fun addForegroundContext(context: Context) {
        mForegroundContexts.add(context)
    }

    fun removeForegroundContext(context: Context) {
        mForegroundContexts.remove(context)
    }

    /**
     * Finds and returns the requested foreground activity, or null if one does not exist.
     * @param contextClass the requested Activity class to find
     * @return the requested foreground activity, or null if one does not exist
     */
    private fun getForegroundContext(contextClass: Class<*>): Context? {
        for (context in mForegroundContexts) {
            if (context.javaClass == contextClass) {
                return context
            }
        }
        return null
    }

    fun isForegroundContext(context: Context): Boolean {
        return getForegroundContext(context.javaClass) != null
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
    override fun onSpeakerphoneOn() {
        Log.i(TAG, "onSpeakerphoneOn()")
    }

    override fun onSpeakerphoneOff() {
        Log.i(TAG, "onSpeakerphoneOff()")
    }

    override fun onWiredHeadsetConnection(state: Int, name: String?, microphone: Int) {
        Log.i(TAG, "onWiredHeadsetConnection(state=$state, name=$name, microphone=$microphone)")
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

    override fun onDockConnection(state: Int) {
        Log.i(TAG, "onDockConnection($state)")

        // If the phone is docked/undocked during a call, and no wired or BT headset
        // is connected: turn on/off the speaker accordingly.
        var inDockMode = false
        if (state != Intent.EXTRA_DOCK_STATE_UNDOCKED) {
            inDockMode = true
        }
        if (VDBG) {
            Log.d(TAG, "inDockMode=$inDockMode")
        }

        /*
        if (shouldTurnOnSpeaker())
        {
            AudioUtils.setSpeakerphoneOn(getApplicationContext(), inDockMode, true);
        }
        */
    }

    override fun onBluetoothHeadsetConnected() {
        Log.i(TAG, "onBluetoothHeadsetConnected()")
        if (audioStateManager.isBluetoothScoAvailableOffCall) {
            audioStateManager.startBluetoothSco()
        }
        mHandler //
                .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
                .sendToTarget()
    }

    override fun onBluetoothHeadsetDisconnected() {
        Log.i(TAG, "onBluetoothHeadsetDisconnected()")

        //if (mAudioStateManager.isBluetoothScoOn())
        //{
        audioStateManager.stopBluetoothSco()
        //}
        mHandler //
                .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
                .sendToTarget()
    }

    override fun onBluetoothHeadsetAudioConnected() {
        Log.i(TAG, "onBluetoothHeadsetAudioConnected()")
        mHandler //
                .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
                .sendToTarget()
    }

    override fun onBluetoothHeadsetAudioDisconnected() {
        Log.i(TAG, "onBluetoothHeadsetAudioDisconnected()")
        mHandler //
                .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
                .sendToTarget()
    }

    private var previousAudioOutputAudioTrackStreamType = -1

    private fun resetPreviousAudioOutputAudioTrackStreamType() {
        Log.e(TAG, "Resetting mPreviousAudioOutputAudioTrackStreamType to -1")
        previousAudioOutputAudioTrackStreamType = -1
    }

    override fun onAudioManagerScoAudioConnected() {
        Log.i(TAG, "onAudioManagerScoAudioConnected()")
        mHandler //
                .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
                .sendToTarget()
        if (previousAudioOutputAudioTrackStreamType != -1) {
            Log.e(TAG, "previousAudioOutputAudioTrackStreamType already set; headset reporting")
            return
        }
        previousAudioOutputAudioTrackStreamType = preferences.audioOutputAudioTrackStreamType
        Log.e(TAG, "Set previousAudioOutputAudioTrackStreamType="
                + AudioStateManager.audioOutputStreamTypeToString(previousAudioOutputAudioTrackStreamType))

        // TODO:(pv) If speaker is playing, close and re-open speaker
        val streamType = AudioManager.STREAM_VOICE_CALL
        Log.e(TAG,
                "Setting mPreferences.putAudioOutputAudioTrackStreamType("
                        + AudioStateManager.audioOutputStreamTypeToString(streamType) + ")")
        preferences.putAudioOutputAudioTrackStreamType(streamType)
        mHandler //
                .obtainMessage(MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE) //
                .sendToTarget()
    }

    override fun onAudioManagerScoAudioDisconnected(error: Boolean) {
        Log.i(TAG, "onAudioManagerScoAudioDisconnected()")
        mHandler //
                .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
                .sendToTarget()

        // Force SCO off to try to repair what appears to be internal BT SCO state bugs in the OS.
        // Sometimes the only way to get SCO to connect is to reboot the phone and/or turn BT off and then back on.
        audioStateManager.stopBluetoothSco()
        if (previousAudioOutputAudioTrackStreamType == -1) {
            Log.e(TAG, "Not restoring previousAudioOutputAudioTrackStreamType because it is not set")
            return
        }
        Log.e(TAG, "Restoring previousAudioOutputAudioTrackStreamType="
                + AudioStateManager.audioOutputStreamTypeToString(previousAudioOutputAudioTrackStreamType))
        val streamType = previousAudioOutputAudioTrackStreamType
        Log.e(TAG,
                "Setting mPreferences.putAudioOutputAudioTrackStreamType("
                        + AudioStateManager.audioOutputStreamTypeToString(streamType) + ")")
        preferences.putAudioOutputAudioTrackStreamType(streamType)
        resetPreviousAudioOutputAudioTrackStreamType()

        // TODO:(pv) If speaker is playing, close and re-open speaker
        mHandler //
                .obtainMessage(MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE) //
                .sendToTarget()
    }

    override fun onAudioPlayerBuffer() {
        if (VDBG) {
            Log.i(TAG, "onAudioPlayerBuffer()")
        }
        onAudioBuffer()
    }

    override fun onAudioRecorderBuffer() {
        if (VDBG) {
            Log.i(TAG, "onAudioRecorderBuffer()")
        }
        onAudioBuffer()
    }

    private fun onAudioBuffer() {
        val audioBufferCount = mAudioBuffers.size()
        val audioBufferPoolCount = mAudioBuffersPool.size()
        if (VDBG) {
            Log.i(TAG, "mAudioBuffers.size()=$audioBufferCount, mAudioBuffersPool.size()=$audioBufferPoolCount")
        }
        mHandler //
                .obtainMessage(MSG_UPDATE_BUFFER_COUNT, audioBufferCount, audioBufferPoolCount) //
                .sendToTarget()
    }

    private fun handleMessage(msg: Message) {
        if (DBG) {
            Log.i(TAG, "handleMessage($msg)")
        }
        val mainActivity = getForegroundContext(MainActivity::class.java) as MainActivity?
        mainActivity?.handleMessage(msg)
    }

    // TODO:(pv) Move below code to a Service class...
    private var mMp3Reader: Mp3Reader? = null
    private var mAudioRecorder: AudioRecorder? = null
    private var mAudioPlayer: AudioPlayer? = null
    private var mAudioPlayerSampleRate = 0
    private var mAudioPlayerChannelConfig = 0
    private var mAudioPlayerEncodingFormat = 0
    private val mAudioBuffers = MyArrayBlockingQueue("QueueAudioBuffers")
    private val mAudioBuffersPool = AudioBufferPool()

    fun audioStopReadingMp3() {
        if (mMp3Reader != null) {
            mMp3Reader!!.stop()
            mMp3Reader = null
        }
    }

    fun audioStartReadingMp3() {
        val audioInputFilePath = preferences.audioInputFilePath
        mMp3Reader = try {
            Mp3Reader(audioInputFilePath, mAudioBuffers, mAudioBuffersPool, this)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "EXCEPTION: new Mp3Reader($audioInputFilePath, ...)", e)
            return
        }
        mAudioPlayerSampleRate = mMp3Reader!!.audioSampleRate
        mAudioPlayerChannelConfig = mMp3Reader!!.audioChannelConfig
        mAudioPlayerEncodingFormat = mMp3Reader!!.audioEncodingFormat
        val thread = Thread(mMp3Reader)
        thread.name = "Mp3Reader"
        thread.start()
    }

    fun audioStopReadingMicrophone() {
        if (mAudioRecorder != null) {
            mAudioRecorder!!.stop()
            mAudioRecorder = null
        }
    }

    fun audioStartReadingMicrophone() {
        val audioInputAudioRecordSourceType = preferences.audioInputAudioRecordSourceType
        mAudioPlayerSampleRate = 8000
        mAudioPlayerChannelConfig = AudioFormat.CHANNEL_OUT_MONO
        mAudioPlayerEncodingFormat = AudioFormat.ENCODING_PCM_16BIT
        mAudioRecorder = AudioRecorder(audioInputAudioRecordSourceType,  //
                mAudioPlayerSampleRate, mAudioPlayerEncodingFormat,  //
                mAudioBuffers, mAudioBuffersPool, this)
        val thread = Thread(mAudioRecorder)
        thread.name = "AudioRecorder"
        thread.start()
    }

    fun audioStopPlayingSource() {
        if (mAudioPlayer != null) {
            mAudioPlayer!!.stop()
            mAudioPlayer = null
        }
    }

    fun audioStartPlayingSource() {
        val audioOutputAudioTrackStreamType = preferences.audioOutputAudioTrackStreamType
        mAudioPlayer = AudioPlayer(audioOutputAudioTrackStreamType,  //
                mAudioPlayerSampleRate, mAudioPlayerChannelConfig, mAudioPlayerEncodingFormat,  //
                mAudioBuffers, mAudioBuffersPool, this)
        val thread = Thread(mAudioPlayer)
        thread.name = "AudioPlayer"
        thread.start()
    }

    companion object {
        @JvmField
        val TAG: String = LoopbackApp::class.java.simpleName

        /**
         * App-wide debug level:
         * 0 - no debug logging
         * 1 - normal debug logging if ro.debuggable is set (which is true in
         * "eng" and "userdebug" builds but not "user" builds)
         * 2 - ultra-verbose debug logging
         *
         * Most individual classes in the phone app have a local DBG constant,
         * typically set to
         * (PhoneApp.DBG_LEVEL >= 1)
         * or else
         * (PhoneApp.DBG_LEVEL >= 2)
         * depending on the desired verbosity.
         *
         * ***** DO NOT RELEASE WITH DBG_LEVEL > 0 *****
         */
        @JvmField
        var DBG_LEVEL = 1
        private val DBG = DBG_LEVEL >= 1
        private val VDBG = DBG_LEVEL >= 2

        /**
         *
         *  * msg.arg1: mAudioBuffers.size()
         *  * msg.arg2: mAudioBuffersPool.size()
         *  * msg.obj: unused
         *
         */
        const val MSG_UPDATE_BUFFER_COUNT = 1

        /**
         *
         *  * msg.arg1: unused
         *  * msg.arg2: unused
         *  * msg.obj: unused
         *
         */
        const val MSG_UPDATE_BLUETOOTH_INDICATION = 2

        /**
         *
         *  * msg.arg1: unused
         *  * msg.arg2: unused
         *  * msg.obj: unused
         *
         */
        const val MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE = 3
    }
}