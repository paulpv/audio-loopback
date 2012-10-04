package com.twistpair.wave.experimental.loopback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.SystemClock;
import android.util.Log;

public class AudioStateManager
{
    private static final String TAG = AudioStateManager.class.getSimpleName();

    public interface AudioStateListener
    {
        void onBluetoothHeadsetConnected();

        void onBluetoothHeadsetDisconnected();

        void onBluetoothHeadsetAudioConnected();

        void onBluetoothHeadsetAudioDisconnected();

        void onAudioManagerScoAudioConnected();

        void onAudioManagerScoAudioDisconnected(boolean error);
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

    public static String bluetoothHeadsetAudioStateToString(int state)
    {
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

    private int                         mBluetoothHeadsetState      = -1;
    private int                         mBluetoothHeadsetAudioState = -1;
    private int                         mAudioManagerScoAudioState  = -1;

    public AudioStateManager(Context context, AudioStateListener listener)
    {
        mContext = context;
        mListener = listener;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
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
            /**
             * Handles the race-condition where startBluetoothSco() sometimes has no effect and/or doesn't fire event if called too soon after headset is connected. 
             */

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

    public boolean isBluetoothHeadsetConnected()
    {
        return mBluetoothHeadsetState == BluetoothHeadset.STATE_CONNECTED;
    }

    public boolean isBluetoothHeadsetAudioConnected()
    {
        return mBluetoothHeadsetAudioState == BluetoothHeadset.STATE_AUDIO_CONNECTED;
    }

    public boolean isAudioManagerScoAudioStateConnected()
    {
        return mAudioManagerScoAudioState == AudioManager.SCO_AUDIO_STATE_CONNECTED;
    }

    private class AudioStateBroadcastReceiver //
                    extends BroadcastReceiver
    {
        public IntentFilter getIntentFilter()
        {
            IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED); // API 11
            intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED); // API 11
            intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED); // API 14

            //intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED); // API 11
            //intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED); // API 11
            //intentFilter.addAction(ACTION_A2DP_SINK_STATE_CHANGED);
            //intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); // API 5
            //intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); // API 5

            return intentFilter;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.d(TAG, "onReceive: intent=" + intent);

            String action = intent.getAction();

            String extras = AppUtils.toString(intent.getExtras());
            Log.d(TAG, "extras=" + extras);

            if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED))
            {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bluetoothHeadsetStatePrevious =
                    intent.getIntExtra(BluetoothHeadset.EXTRA_PREVIOUS_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                int bluetoothHeadsetState =
                    intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);

                Log.d(TAG, "mReceiver: BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED");
                Log.d(TAG, "==> bluetoothDevice=" + bluetoothDevice);
                Log.d(TAG, "==> bluetoothHeadsetStatePrevious=" + bluetoothHeadsetStateToString(bluetoothHeadsetStatePrevious));
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

                return;
            }

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
                Log.d(TAG, "==> bluetoothHeadsetAudioState=" + bluetoothHeadsetAudioStateToString(bluetoothHeadsetAudioState));

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

                return;
            }

            if (action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
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

                        mListener.onAudioManagerScoAudioConnected();
                        break;
                    }

                    case AudioManager.SCO_AUDIO_STATE_ERROR:
                    case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                    {
                        // Async call to AudioManager.stopBluetoothSco() completed successfully...
                        // ...but the app should have actively stopped using SCO the moment stopBluetoothSco() was called...
                        // ...or AudioManager.startBluetoothSco() failed.

                        // App should consider moving back to previous stream type [AudioManager.STREAM_MUSIC?].

                        boolean error = scoAudioState == AudioManager.SCO_AUDIO_STATE_ERROR;
                        mListener.onAudioManagerScoAudioDisconnected(error);
                        break;
                    }
                }

                return;
            }
        }
    }
}
