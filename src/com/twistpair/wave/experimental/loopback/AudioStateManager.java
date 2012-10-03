package com.twistpair.wave.experimental.loopback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

/**
 * @author Paul Peavyhouse (pv@twistpair.com)
 * 
 * Attempt at an API 8 compatible Bluetooth/Speaker/Handset/Headset audio routing manager.
 * 
 * NOTE that all the code in this class should have basic functionality on API 8 (even if it is a bit limited).
 * 
 * There story is confusing here because Google neglected the Bluetooth API for the longest time.<br>
 * Google only really started to clean it up in API 11.<br>
 * For example, the official Android docs for {@link BluetoothHeadset} say "Since: API Level 11".<br>
 * However, the two most official links for API 8 source code show that BluetoothHeadset exists:
 * <ul>
 * <li><a href="https://android.googlesource.com/platform/frameworks/base/+/froyo-release/core/java/android/bluetooth/BluetoothHeadset.java">Android GoogleSource</a></li>
 * <li><a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java">GitHub Android</a></li>
 * </ul>
 * 
 * I take this to mean that pre-API 11, BluetoothHeadset (and others) existed but were not stable or official supported (per its scary header comments).<br>
 * Keep this in mind when trying to make sense of the below code.
 *  
 * Ideas came from Android Phone App and Bluetooth Core:
 *  https://github.com/android/platform_packages_apps_phone/blob/master/src/com/android/phone/*.java
 *  https://github.com/android/platform_frameworks_base/blob/master/core/java/android/bluetooth/*.java
 * Specifically:
 *  https://github.com/android/platform_packages_apps_phone/blob/master/src/com/android/phone/PhoneUtils.java
 *  https://github.com/android/platform_frameworks_base/blob/master/core/java/android/bluetooth/BluetoothHeadset.java
 */
public class AudioStateManager
{
    private static final String TAG = AudioStateManager.class.getSimpleName();

    //private static final boolean DBG  = (LoopbackApp.DBG_LEVEL >= 1);
    //private static final boolean VDBG = (LoopbackApp.DBG_LEVEL >= 2);

    public interface AudioStateListener
    {
        void onSpeakerphoneOn();

        void onSpeakerphoneOff();

        /**
         * {@link Intent#ACTION_HEADSET_PLUG} 
         * @param state 0 for unplugged, 1 for plugged
         * @param name Headset type, human readable string
         * @param microphone 1 if headset has a microphone, 0 otherwise
         */
        void onWiredHeadsetConnection(int state, String name, int microphone);

        /**
         * {@link Intent#ACTION_DOCK_EVENT} 
         * @param state One of Intent.EXTRA_DOCK_STATE_*
         */
        void onDockConnection(int state);

        void onBluetoothHeadsetConnected();

        void onBluetoothHeadsetDisconnected();

        void onBluetoothHeadsetAudioConnected();

        void onBluetoothHeadsetAudioDisconnected();

        void onAudioManagerScoAudioConnected();

        void onAudioManagerScoAudioDisconnected(boolean error);

        //void onBluetoothDeviceAclConnected(BluetoothDevice bluetoothDevice);

        //void onBluetoothDeviceAclDisconnected(BluetoothDevice bluetoothDevice);
    }

    public static String audioOutputStreamTypeToString(int streamType)
    {
        switch (streamType)
        {
            case AudioManager.STREAM_ALARM: // API 1
                return "STREAM_ALARM(" + streamType + ")";
            case AudioManager.STREAM_DTMF: // API 5
                return "STREAM_DTMF(" + streamType + ")";
            case AudioManager.STREAM_MUSIC: // API 1
                return "STREAM_MUSIC(" + streamType + ")";
            case AudioManager.STREAM_NOTIFICATION: // API 3
                return "STREAM_NOTIFICATION(" + streamType + ")";
            case AudioManager.STREAM_RING: // API 1
                return "STREAM_RING(" + streamType + ")";
            case AudioManager.STREAM_SYSTEM: // API 1
                return "STREAM_SYSTEM(" + streamType + ")";
            case AudioManager.STREAM_VOICE_CALL: // API 1
                return "STREAM_VOICE_CALL(" + streamType + ")";
            default:
                return "UNKNOWN(" + streamType + ")";
        }
    }

    public static String audioInputAudioSourceToString(int audioSource)
    {
        switch (audioSource)
        {
            case MediaRecorder.AudioSource.CAMCORDER:
                return "CAMCORDER(" + audioSource + ")";
            case MediaRecorder.AudioSource.DEFAULT:
                return "DEFAULT(" + audioSource + ")";
            case MediaRecorder.AudioSource.MIC:
                return "MIC(" + audioSource + ")";
            case MediaRecorder.AudioSource.VOICE_CALL:
                return "VOICE_CALL(" + audioSource + ")";
            case MediaRecorder.AudioSource.VOICE_COMMUNICATION:
                return "VOICE_COMMUNICATION(" + audioSource + ")";
            case MediaRecorder.AudioSource.VOICE_DOWNLINK:
                return "VOICE_DOWNLINK(" + audioSource + ")";
            case MediaRecorder.AudioSource.VOICE_RECOGNITION:
                return "VOICE_RECOGNITION(" + audioSource + ")";
            case MediaRecorder.AudioSource.VOICE_UPLINK:
                return "VOICE_UPLINK(" + audioSource + ")";
            default:
                return "UNKNOWN(" + audioSource + ")";
        }
    }

    public static String bluetoothHeadsetStateToString(int state)
    {
        switch (Build.VERSION.SDK_INT)
        {
            case 8:
                switch (state)
                {
                    case AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_STATE_ERROR_8:
                        return "STATE_ERROR(" + state + ")";
                    case AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_STATE_DISCONNECTED_8:
                        return "STATE_DISCONNECTED(" + state + ")";
                    case AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_STATE_CONNECTING_8:
                        return "STATE_CONNECTING(" + state + ")";
                    case AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_STATE_CONNECTED_8:
                        return "STATE_CONNECTED(" + state + ")";
                    default:
                        return "UNKNOWN(" + state + ")";
                }
            default:
                switch (state)
                {
                    case BluetoothHeadset.STATE_DISCONNECTED:
                        return "STATE_DISCONNECTED(" + state + ")";
                    case BluetoothHeadset.STATE_CONNECTING:
                        return "STATE_CONNECTING(" + state + ")";
                    case BluetoothHeadset.STATE_CONNECTED:
                        return "STATE_CONNECTED(" + state + ")";
                    case BluetoothHeadset.STATE_DISCONNECTING:
                        return "STATE_DISCONNECTING(" + state + ")";
                    default:
                        return "UNKNOWN(" + state + ")";
                }
        }
    }

    public static String bluetoothHeadsetAudioStateToString(int state)
    {
        switch (Build.VERSION.SDK_INT)
        {
            case 8:
                switch (state)
                {
                    case AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_AUDIO_STATE_DISCONNECTED_8:
                        return "AUDIO_STATE_DISCONNECTED(" + state + ")";
                    case AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_AUDIO_STATE_CONNECTED_8:
                        return "AUDIO_STATE_CONNECTED(" + state + ")";
                    default:
                        return "UNKNOWN(" + state + ")";
                }
            default:
                switch (state)
                {
                    case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                        return "STATE_AUDIO_DISCONNECTED(" + state + ")";
                    case BluetoothHeadset.STATE_AUDIO_CONNECTING:
                        return "STATE_AUDIO_CONNECTING(" + state + ")";
                    case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                        return "STATE_AUDIO_CONNECTED(" + state + ")";
                    default:
                        return "UNKNOWN(" + state + ")";
                }
        }
    }

    public static String audioManagerScoAudioStateToString(int state)
    {
        switch (state)
        {
            case AudioManager.SCO_AUDIO_STATE_ERROR: // API 8
                return "SCO_AUDIO_STATE_ERROR(" + state + ")";
            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED: // API 8
                return "SCO_AUDIO_STATE_DISCONNECTED(" + state + ")";
            case AudioManager.SCO_AUDIO_STATE_CONNECTED: // API 8
                return ".SCO_AUDIO_STATE_CONNECTED(" + state + ")";
            case AudioManager.SCO_AUDIO_STATE_CONNECTING: // API 14
                return "SCO_AUDIO_STATE_CONNECTING(" + state + ")";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    public static String dockStateToString(int state)
    {
        switch (state)
        {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED: // API 5
                return "UNDOCKED(" + state + ")";
            case Intent.EXTRA_DOCK_STATE_DESK: // API 5
                return "DESK(" + state + ")";
            case Intent.EXTRA_DOCK_STATE_CAR: // API 5
                return "CAR(" + state + ")";
            case Intent.EXTRA_DOCK_STATE_LE_DESK: // API 11
                return "ANALOG_DESK(" + state + ")";
            case Intent.EXTRA_DOCK_STATE_HE_DESK: // API 11
                return "DIGITAL_DESK(" + state + ")";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }

    private final Context               mContext;
    private final AudioStateListener    mListener;
    private final AudioManager          mAudioManager;

    private AudioStateBroadcastReceiver mAudioStateBroadcastReceiver;

    /** Control stack trace for Audio Mode settings */
    //private final boolean                     DBG_SETAUDIOMODE_STACK      = false;

    /** Speaker state, persisting between wired headset connection events */
    //private boolean                     mIsSpeakerphoneOn;

    /** Noise suppression status as selected by user */
    //private boolean                     mIsNoiseSuppressionOn;

    /**
     * Managed by BroadcastReceiver for Intent.ACTION_HEADSET_PLUG
     */
    //private boolean                     mIsWiredHeadsetPlugged;

    /**
     * Managed by BroadcastReceiver for Intent.ACTION_DOCK_EVENT
     */
    //private int                         mDockState                  = Intent.EXTRA_DOCK_STATE_UNDOCKED;

    //private int                               mBluetoothDeviceState2       = BluetoothAdapter.STATE_DISCONNECTED;

    /**
     * 
     */
    private int                         mBluetoothHeadsetState      = -1;

    /**
     * 
     */
    private int                         mBluetoothHeadsetAudioState = -1;

    /**
     * One of AudioManager.SCO_AUDIO_STATE_*
     */
    private int                         mAudioManagerScoAudioState  = -1;

    public AudioStateManager(Context context, AudioStateListener listener)
    {
        // TODO:(pv) Use a Handler pattern instead of a Listener pattern?
        mContext = context;
        mListener = listener;

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        //mIsSpeakerphoneOn = isSpeakerphoneOn();
        //mIsNoiseSuppressionOn = isNoiseSuppressionOn();
        // mIsWiredHeadsetPlugged will be populated by sticky broadcast receiver
    }

    public void stop()
    {
        if (mAudioStateBroadcastReceiver != null)
        {
            mContext.unregisterReceiver(mAudioStateBroadcastReceiver);
            mAudioStateBroadcastReceiver = null;
        }
    }

    public void start()
    {
        stop();

        mAudioStateBroadcastReceiver = new AudioStateBroadcastReceiver();
        IntentFilter intentFilter = mAudioStateBroadcastReceiver.getIntentFilter();
        mContext.registerReceiver(mAudioStateBroadcastReceiver, intentFilter);
    }

    public boolean isSpeakerphoneOn()
    {
        return mAudioManager.isSpeakerphoneOn(); // API 1
    }

    public void setSpeakerphoneOn(boolean on)//, boolean store)
    {
        Log.d(TAG, "setSpeakerphoneOn(on=" + on + ")");//, store=" + store + ")");

        mAudioManager.setSpeakerphoneOn(on); // API 1

        /*
        // Used in restoreSpeakerphone()
        if (store)
        {
            mIsSpeakerphoneOn = on;
        }
        */

        // TODO:(pv) Bad coupling of code w/ UI logic?
        //final LoopbackApp app = LoopbackApp.getInstance();

        // Update the status bar icon
        //app.getNotificationManager().updateSpeakerNotification(on);

        // We also need to make a fresh call to PhoneApp.updateWakeState()
        // any time the speaker state changes, since the screen timeout is
        // sometimes different depending on whether or not the speaker is
        // in use.
        //app.updateWakeState();

        // Update the Proximity sensor based on speaker state
        //app.updateProximitySensorMode(app.mCM.getState());

        //app.mCM.setEchoSuppressionEnabled(flag);

        /*
        if (on)
        {
            mListener.onSpeakerphoneOn();
        }
        else
        {
            mListener.onSpeakerphoneOff();
        }
        */
    }

    /*
    public void restoreSpeakerphone()
    {
        Log.d(TAG, "restoreSpeakerphone, restoring to: " + mIsSpeakerphoneOn);
        if (isSpeakerphoneOn() != mIsSpeakerphoneOn)
        {
            setSpeakerphoneOn(mIsSpeakerphoneOn, false);
        }
    }
    */

    /*
    public boolean findBluetoothHeadsetConnected()
    {
        BluetoothAdapter mBluetoothAdapter = null;
        try
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null)
            {
                Log.w(TAG, "Device does not support Bluetooth");
                return false;
            }
        }
        catch (RuntimeException e)
        {
            Log.w(TAG, "Can't get default bluetooth adapter", e);
            return false;
        }

        boolean hasConnectedDevice = false;

        if (mBluetoothAdapter.isEnabled())
        {
            // We get all bounded bluetooth devices
            // bounded is not enough, should search for connected devices....
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices)
            {
                BluetoothClass bluetoothClass = device.getBluetoothClass();
                if (bluetoothClass != null)
                {
                    int deviceClass = bluetoothClass.getDeviceClass();

                    if (bluetoothClass.hasService(Service.RENDER) //
                                    && (deviceClass == Device.AUDIO_VIDEO_WEARABLE_HEADSET
                                                    || deviceClass == Device.AUDIO_VIDEO_CAR_AUDIO || deviceClass == Device.AUDIO_VIDEO_HANDSFREE))
                    {
                        // And if any can be used as a audio handset
                        hasConnectedDevice = true;
                        break;
                    }
                }
            }
        }

        return hasConnectedDevice && mAudioManager.isBluetoothScoAvailableOffCall();
    }
    */

    public boolean isBluetoothScoAvailableOffCall()
    {
        boolean isBluetoothScoAvailableOffCall = mAudioManager.isBluetoothScoAvailableOffCall(); // API 8
        Log.d(TAG, "mAudioManager.isBluetoothScoAvailableOffCall()=" + isBluetoothScoAvailableOffCall);
        return isBluetoothScoAvailableOffCall;
    }

    public boolean isBluetoothScoOn()
    {
        boolean isBluetoothScoOn = mAudioManager.isBluetoothScoOn(); // API 1
        Log.d(TAG, "mAudioManager.isBluetoothScoOn()=" + isBluetoothScoOn);
        return isBluetoothScoOn;
    }

    public void setBluetoothScoOn(boolean on)
    {
        Log.d(TAG, "mAudioManager.setBluetoothScoOn(" + on + ")");
        mAudioManager.setBluetoothScoOn(on); // API 1
    }

    public int getMode()
    {
        int mode = mAudioManager.getMode(); // API 1 
        Log.d(TAG, "mAudioManager.getMode()=" + mode);
        return mode;
    }

    public void setMode(int mode)
    {
        Log.d(TAG, "mAudioManager.setMode(" + mode + ")");
        mAudioManager.setMode(mode); // API 1
    }

    /**
     * Handles the race-condition where startBluetoothSco() sometimes has no effect and/or doesn't fire event if called too soon after headset is connected. 
     */
    public void startBluetoothSco()
    {
        Log.d(TAG, "startBluetoothSco()");

        //setBluetoothScoOn(true);

        if (true)
        {
            Log.i(TAG, "mAudioManager.startBluetoothSco();");
            mAudioManager.startBluetoothSco(); // API 8
        }
        else
        {
            final long max = 5000;
            final long start = SystemClock.elapsedRealtime();

            Thread thread = new Thread()
            {
                @Override
                public void run()
                {
                    long now = SystemClock.elapsedRealtime();
                    long elapsed = now - start;

                    int attemptNumber = 0;

                    while (true)
                    {
                        attemptNumber++;
                        Log.i(TAG, "startBluetoothSco() attempt #" + attemptNumber);

                        if (isBluetoothHeadsetConnected())
                        {
                            Log.i(TAG, "Found Bluetooth headset");
                            if (isBluetoothScoOn())
                            {
                                Log.w(TAG, "isBluetoothScoOn()==true after " + attemptNumber + " attempts over " + elapsed
                                                + " ms");

                                mAudioManagerScoAudioState = AudioManager.SCO_AUDIO_STATE_CONNECTED;
                                mListener.onAudioManagerScoAudioConnected();
                                return;
                            }
                            else
                            {
                                Log.i(TAG, "isBluetoothScoOn()==false; mAudioManager.startBluetoothSco();");
                                mAudioManager.startBluetoothSco(); // API 8
                            }
                        }
                        else
                        {
                            Log.i(TAG, "Waiting for Bluetooth headset");
                        }

                        now = SystemClock.elapsedRealtime();
                        elapsed = now - start;
                        if (elapsed > max)
                        {
                            Log.i(TAG, "startBluetoothSco(): isBluetoothScoOn()==false after " + max + " ms");
                            return;
                        }

                        try
                        {
                            Log.i(TAG, "startBluetoothSco(): sleep(100);");
                            sleep(100);
                        }
                        catch (InterruptedException e)
                        {
                            // ignore
                            return;
                        }
                    }
                }
            };
            thread.setName("AsyncStartBluetoothSco");
            thread.start();
        }
    }

    public void stopBluetoothSco()
    {
        Log.d(TAG, "stopBluetoothSco()");

        //setBluetoothScoOn(false);

        Log.d(TAG, "mAudioManager.stopBluetoothSco()");
        mAudioManager.stopBluetoothSco(); // API 8
    }

    /*
    public boolean isWiredHeadsetPlugged()
    {
        return mIsWiredHeadsetPlugged;
    }
    */

    /*
    public void turnOnNoiseSuppression(boolean on, boolean store)
    {
        Log.d(TAG, "turnOnNoiseSuppression(on=" + on + ", store=" + store + ")");

        mAudioManager.setParameters("noise_suppression=" + (on ? "auto" : "off")); // API 5

        if (store)
        {
            mIsNoiseSuppressionOn = on;
        }
    }

    public void restoreNoiseSuppression()
    {
        Log.d(TAG, "restoreNoiseSuppression, restoring to: " + mIsNoiseSuppressionOn);
        if (isNoiseSuppressionOn() != mIsNoiseSuppressionOn)
        {
            turnOnNoiseSuppression(mIsNoiseSuppressionOn, false);
        }
    }

    public boolean isNoiseSuppressionOn()
    {
        String noiseSuppression = mAudioManager.getParameters("noise_suppression"); // API 5
        Log.d(TAG, "noiseSuppression=\"" + noiseSuppression + "\"");
        return noiseSuppression.contains("off") == false;
    }
    */

    public boolean isBluetoothHeadsetConnected()
    {
        if (Build.VERSION.SDK_INT == 8)
        {
            return mBluetoothHeadsetState == AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_STATE_CONNECTED_8;
        }
        else
        {
            return mBluetoothHeadsetState == BluetoothHeadset.STATE_CONNECTED;
        }
    }

    public boolean isBluetoothHeadsetAudioConnected()
    {
        if (Build.VERSION.SDK_INT == 8)
        {
            return mBluetoothHeadsetAudioState == AudioStateBroadcastReceiver.BLUETOOTH_HEADSET_AUDIO_STATE_CONNECTED_8;
        }
        else
        {
            return mBluetoothHeadsetAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED;
        }
    }

    public boolean isAudioManagerScoAudioStateConnected()
    {
        return mAudioManagerScoAudioState == AudioManager.SCO_AUDIO_STATE_CONNECTED;
    }

    private class AudioStateBroadcastReceiver //
                    extends BroadcastReceiver
    {
        /**
         * <p>{@link BluetoothHeadset#ACTION_STATE_CHANGED} (API 8) is deprecated by {@link BluetoothHeadset#ACTION_CONNECTION_STATE_CHANGED} (API 11)</p>
         * <p>Usage: Not officially supported?<br>
         * I am guessing that this intent has two extras:
         * <ul>
         * <li>{@link #BLUETOOTH_HEADSET_EXTRA_STATE_8}<//li>
         * <li>{@link #BLUETOOTH_HEADSET_EXTRA_PREVIOUS_STATE_8}</li>
         * </ul></p>
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L60">BluetoothHeadset.ACTION_STATE_CHANGED</a> = "android.bluetooth.headset.action.STATE_CHANGED";</p>
         */
        private static final String BLUETOOTH_HEADSET_ACTION_STATE_CHANGED_8       =
                                                                                       "android.bluetooth.headset.action.STATE_CHANGED";

        /**
         * <p>{@link BluetoothHeadset#ACTION_AUDIO_STATE_CHANGED} (API 8) is deprecated by {@link BluetoothHeadset#ACTION_AUDIO_STATE_CHANGED} (API 11)</p>
         * <p>Usage: Not officially supported?<br>
         * I am guessing that this intent has one extra:
         * <ul>
         * <li>{@link #BLUETOOTH_HEADSET_EXTRA_AUDIO_STATE_8}<//li>
         * </ul></p>
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L67">BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED</a> = "android.bluetooth.headset.action.AUDIO_STATE_CHANGED";<br>
         */
        public static final String  BLUETOOTH_HEADSET_ACTION_AUDIO_STATE_CHANGED_8 =
                                                                                       "android.bluetooth.headset.action.AUDIO_STATE_CHANGED";

        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L69">BluetoothHeadset.EXTRA_STATE</a> = "android.bluetooth.headset.extra.STATE";<br>
         */
        private static final String BLUETOOTH_HEADSET_EXTRA_STATE_8                = "android.bluetooth.headset.extra.STATE";
        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L71">BluetoothHeadset.EXTRA_PREVIOUS_STATE</a> = "android.bluetooth.headset.extra.PREVIOUS_STATE";<br>
         */
        private static final String BLUETOOTH_HEADSET_EXTRA_PREVIOUS_STATE_8       =
                                                                                       "android.bluetooth.headset.extra.PREVIOUS_STATE";
        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L73">BluetoothHeadset.EXTRA_AUDIO_STATE</a> = "android.bluetooth.headset.extra.AUDIO_STATE";<br>
         */
        private static final String BLUETOOTH_HEADSET_EXTRA_AUDIO_STATE_8          =
                                                                                       "android.bluetooth.headset.extra.AUDIO_STATE";

        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L96">BluetoothHeadset.STATE_ERROR</a> = -1;<br>
         */
        private static final int    BLUETOOTH_HEADSET_STATE_ERROR_8                = -1;
        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L98">BluetoothHeadset.STATE_DISCONNECTED</a> = 0;<br>
         */
        private static final int    BLUETOOTH_HEADSET_STATE_DISCONNECTED_8         = 0;
        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L100">BluetoothHeadset.STATE_CONNECTING</a> = 1;<br>
         */
        private static final int    BLUETOOTH_HEADSET_STATE_CONNECTING_8           = 1;
        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L102">BluetoothHeadset.STATE_CONNECTED</a> = 2;<br>
         */
        private static final int    BLUETOOTH_HEADSET_STATE_CONNECTED_8            = 2;

        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L105">BluetoothHeadset.AUDIO_STATE_DISCONNECTED</a> = 0;<br>
         */
        private static final int    BLUETOOTH_HEADSET_AUDIO_STATE_DISCONNECTED_8   = 0;
        /**
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/core/java/android/bluetooth/BluetoothHeadset.java#L107">BluetoothHeadset.AUDIO_STATE_CONNECTED</a> = 1;<br>
         */
        private static final int    BLUETOOTH_HEADSET_AUDIO_STATE_CONNECTED_8      = 1;

        /**
         * <p>{@link AudioManager#ACTION_SCO_AUDIO_STATE_CHANGED} (API 8) is deprecated by {@link AudioManager#ACTION_SCO_AUDIO_STATE_UPDATED} (API 14)</p>
         * <p>Usage: "Sticky broadcast intent action indicating that the bluetooth SCO audio connection state has changed.
         * The intent contains on extra {@link AudioManager#EXTRA_SCO_AUDIO_STATE} indicating the new state which is either {@link AudioManager#SCO_AUDIO_STATE_DISCONNECTED} or {@link AudioManager#SCO_AUDIO_STATE_CONNECTED}."</p>
         * <p>Captured here for so that API 8 devices can handle this broadcast event (in case it is removed in future APIs).</p>
         * <p>API 8 code (unsupported?): <a href="https://github.com/android/platform_frameworks_base/blob/froyo/media/java/android/media/AudioManager.java#L704">AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED</a> = "android.media.SCO_AUDIO_STATE_CHANGED"</p>
         */
        private static final String AUDIO_MANAGER_ACTION_SCO_AUDIO_STATE_CHANGED_8 = "android.media.SCO_AUDIO_STATE_CHANGED";

        /**
         * 
         */
        //private static final String ACTION_A2DP_SINK_STATE_CHANGED                       =
        //                                                                                     "android.bluetooth.a2dp.action.SINK_STATE_CHANGED";

        public IntentFilter getIntentFilter()
        {
            // We need to support API >= 8
            IntentFilter intentFilter = new IntentFilter();

            if (Build.VERSION.SDK_INT == 8)
            {
                intentFilter.addAction(BLUETOOTH_HEADSET_ACTION_STATE_CHANGED_8); // API 8
                intentFilter.addAction(BLUETOOTH_HEADSET_ACTION_AUDIO_STATE_CHANGED_8); // API 8
                intentFilter.addAction(AUDIO_MANAGER_ACTION_SCO_AUDIO_STATE_CHANGED_8); // API 8
            }
            else
            {
                intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); // API 11
                intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED); // API 11
                intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED); // API 14
            }

            //intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED); // API 11
            //intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED); // API 11
            //intentFilter.addAction(ACTION_A2DP_SINK_STATE_CHANGED);
            //intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); // API 5
            //intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); // API 5
            intentFilter.addAction(Intent.ACTION_HEADSET_PLUG); // API 1
            intentFilter.addAction(Intent.ACTION_DOCK_EVENT); // API 5
            //intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED); // Redundant/Ignore: Entering/Exiting will broadcast the others
            return intentFilter;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "onReceive: intent=" + intent);

            String action = intent.getAction();

            String extras = AppUtils.toString(intent.getExtras());
            Log.d(TAG, "extras=" + extras);

            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                            || action.equals(BLUETOOTH_HEADSET_ACTION_STATE_CHANGED_8))
            {
                if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int bluetoothHeadsetStatePrevious =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                    int bluetoothHeadsetState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);

                    Log.d(TAG, "mReceiver: BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED");
                    Log.d(TAG, "==> bluetoothDevice=" + bluetoothDevice);
                    Log.d(TAG, "==> bluetoothHeadsetStatePrevious="
                                    + bluetoothHeadsetStateToString(bluetoothHeadsetStatePrevious));
                    Log.d(TAG, "==> bluetoothHeadsetState=" + bluetoothHeadsetStateToString(bluetoothHeadsetState));

                    mBluetoothHeadsetState = bluetoothHeadsetState;

                    switch (bluetoothHeadsetState)
                    {
                        case BluetoothHeadset.STATE_CONNECTED:
                            mListener.onBluetoothHeadsetConnected();
                            break;
                        case BluetoothHeadset.STATE_DISCONNECTED:
                            mListener.onBluetoothHeadsetDisconnected();
                            break;
                    }
                }
                else if (action.equals(BLUETOOTH_HEADSET_ACTION_STATE_CHANGED_8))
                {
                    int bluetoothHeadsetStatePrevious =
                        intent.getIntExtra(BLUETOOTH_HEADSET_EXTRA_PREVIOUS_STATE_8, BLUETOOTH_HEADSET_STATE_DISCONNECTED_8);
                    int bluetoothHeadsetState =
                        intent.getIntExtra(BLUETOOTH_HEADSET_EXTRA_STATE_8, BLUETOOTH_HEADSET_STATE_DISCONNECTED_8);

                    Log.d(TAG, "mReceiver: BLUETOOTH_HEADSET_ACTION_STATE_CHANGED_8");
                    Log.d(TAG, "==> bluetoothHeadsetStatePrevious="
                                    + bluetoothHeadsetStateToString(bluetoothHeadsetStatePrevious));
                    Log.d(TAG, "==> bluetoothHeadsetState=" + bluetoothHeadsetStateToString(bluetoothHeadsetState));

                    mBluetoothHeadsetState = bluetoothHeadsetState;

                    switch (bluetoothHeadsetState)
                    {
                        case BLUETOOTH_HEADSET_STATE_CONNECTED_8:
                            mListener.onBluetoothHeadsetConnected();
                            break;
                        case BLUETOOTH_HEADSET_STATE_DISCONNECTED_8:
                            mListener.onBluetoothHeadsetDisconnected();
                            break;
                    }
                }

                return;
            }

            if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
                            || action.equals(BLUETOOTH_HEADSET_ACTION_AUDIO_STATE_CHANGED_8))
            {
                if (action.equals(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED))
                {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int bluetoothHeadsetAudioStatePrevious =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);
                    int bluetoothHeadsetAudioState =
                        intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED);

                    Log.d(TAG, "mReceiver: BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED");
                    Log.d(TAG, "==> bluetoothDevice=" + bluetoothDevice);
                    Log.d(TAG, "==> bluetoothHeadsetAudioStatePrevious="
                                    + bluetoothHeadsetAudioStateToString(bluetoothHeadsetAudioStatePrevious));
                    Log.d(TAG, "==> bluetoothHeadsetAudioState="
                                    + bluetoothHeadsetAudioStateToString(bluetoothHeadsetAudioState));

                    mBluetoothHeadsetAudioState = bluetoothHeadsetAudioState;

                    switch (bluetoothHeadsetAudioState)
                    {
                        case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                            mListener.onBluetoothHeadsetAudioConnected();
                            break;
                        case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                        {
                            mListener.onBluetoothHeadsetAudioDisconnected();
                            break;
                        }
                    }
                }
                else if (action.equals(BLUETOOTH_HEADSET_ACTION_AUDIO_STATE_CHANGED_8))
                {
                    int bluetoothHeadsetAudioState =
                        intent.getIntExtra(BLUETOOTH_HEADSET_EXTRA_AUDIO_STATE_8, BLUETOOTH_HEADSET_AUDIO_STATE_DISCONNECTED_8);

                    Log.d(TAG, "mReceiver: BLUETOOTH_HEADSET_ACTION_AUDIO_STATE_CHANGED_8");
                    Log.d(TAG, "==> bluetoothHeadsetAudioState="
                                    + bluetoothHeadsetAudioStateToString(bluetoothHeadsetAudioState));

                    mBluetoothHeadsetAudioState = bluetoothHeadsetAudioState;

                    switch (bluetoothHeadsetAudioState)
                    {
                        case BLUETOOTH_HEADSET_AUDIO_STATE_CONNECTED_8:
                            mListener.onBluetoothHeadsetAudioConnected();
                            break;
                        case BLUETOOTH_HEADSET_AUDIO_STATE_DISCONNECTED_8:
                            mListener.onBluetoothHeadsetAudioDisconnected();
                            break;
                    }
                }

                return;
            }

            if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                            || action.equals(AUDIO_MANAGER_ACTION_SCO_AUDIO_STATE_CHANGED_8))
            {
                int scoAudioStatePrevious = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE, -1);
                int scoAudioState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);

                Log.d(TAG, action);
                Log.d(TAG, "==> scoAudioStatePrevious=" + audioManagerScoAudioStateToString(scoAudioStatePrevious));
                Log.d(TAG, "==> scoAudioState=" + audioManagerScoAudioStateToString(scoAudioState));

                mAudioManagerScoAudioState = scoAudioState;

                switch (scoAudioState)
                {
                    case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                    {
                        // Async call to AudioManager.startBluetoothSco() completed successfully

                        // App should consider moving to AudioManager.STREAM_VOICE_CALL mode

                        Log.d(TAG, action + ": SCO_AUDIO_STATE_CONNECTED");
                        mListener.onAudioManagerScoAudioConnected();
                        break;
                    }

                    case AudioManager.SCO_AUDIO_STATE_ERROR:
                    case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                    {
                        // Async call to AudioManager.stopBluetoothSco() completed successfully...
                        // ...but the app should have actively stopped using SCO the moment stopBluetoothSco() was called...
                        // ...or AudioManager.startBluetoothSco() failed.

                        // App should consider moving [back] to AudioManager.STREAM_MUSIC mode

                        Log.d(TAG, action + ": SCO_AUDIO_STATE_DISCONNECTED");
                        boolean error = scoAudioState == AudioManager.SCO_AUDIO_STATE_ERROR;
                        mListener.onAudioManagerScoAudioDisconnected(error);
                        break;
                    }
                }

                return;
            }

            /*
            
            //
            // QUESTION:(pv) Only needed for serial devices?!?!
            //
            
            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
            {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // TODO:(pv) App should consider moving to AudioManager.STREAM_VOICE_CALL mode

                Log.e(TAG, "mReceiver: BluetoothDevice.ACTION_ACL_CONNECTED");
                mBluetoothDeviceState = BluetoothProfile.STATE_CONNECTED;
                mListener.onBluetoothDeviceAclConnected(bluetoothDevice);
                return;
            }

            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // TODO:(pv) App should consider moving to AudioManager.STREAM_MUSIC mode

                Log.e(TAG, "mReceiver: BluetoothDevice.ACTION_ACL_DISCONNECTED");
                mBluetoothDeviceState = BluetoothProfile.STATE_DISCONNECTED;
                mListener.onBluetoothDeviceAclDisconnected(bluetoothDevice);
                return;
            }
            */

            /*
            if (action.equals(ACTION_A2DP_SINK_STATE_CHANGED))
            {
                Log.d(TAG, "ACTION_A2DP_SINK_STATE_CHANGED ---");
                //app.getMessageHandler().sendEmptyMessage(Constants.AudioSteering.CHECK_HEADSET2);
                return;
            }
            */

            if (action.equals(Intent.ACTION_HEADSET_PLUG))
            {
                int state = intent.getIntExtra("state", 0);
                String name = intent.getStringExtra("name");
                int microphone = intent.getIntExtra("microphone", 0);
                Log.d(TAG, "mReceiver: ACTION_HEADSET_PLUG");
                Log.d(TAG, "==> state=" + state);
                Log.d(TAG, "==> name=" + name);
                Log.d(TAG, "==> microphone=" + microphone);

                //mIsWiredHeadsetPlugged = (state == 1);

                mListener.onWiredHeadsetConnection(state, name, microphone);
                return;
            }

            if (action.equals(Intent.ACTION_DOCK_EVENT))
            {
                int dockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, Intent.EXTRA_DOCK_STATE_UNDOCKED);
                Log.d(TAG, "mReceiver: ACTION_DOCK_EVENT");
                Log.d(TAG, "==> dockState=" + AudioStateManager.dockStateToString(dockState));

                //mDockState = dockState;

                mListener.onDockConnection(dockState);
                return;
            }
        }
    }
}
