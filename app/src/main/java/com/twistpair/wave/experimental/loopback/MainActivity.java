package com.twistpair.wave.experimental.loopback;

import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;

import com.twistpair.wave.experimental.loopback.AudioStateManager.AudioStateListener;

public class MainActivity //
                extends //
                FragmentActivity //
                implements //
                AudioStateListener //
{
    private static final String  TAG                                 = MainActivity.class.getSimpleName();

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
    public static int            DBG_LEVEL                           = 1;

    private static final boolean DBG                                 = (MainActivity.DBG_LEVEL >= 1);
    private static final boolean VDBG                                = (MainActivity.DBG_LEVEL >= 2);

    /**
     * <ul>
     * <li>msg.arg1: unused</li>
     * <li>msg.arg2: unused</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int      MSG_UPDATE_BLUETOOTH_INDICATION     = 1;

    /**
     * <ul>
     * <li>msg.arg1: unused</li>
     * <li>msg.arg2: unused</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int      MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE = 2;

    private Handler              mHandler;
    private AudioStateManager    mAudioStateManager;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler()
        {
            @Override
            public void handleMessage(Message msg)
            {
                MainActivity.this.handleMessage(msg);
            }
        };

        mAudioStateManager = new AudioStateManager(this, this);
        mAudioStateManager.start();

        //
        //
        //
        Button buttonStartBluetoothSco = (Button) findViewById(R.id.buttonStartBluetoothSco);
        buttonStartBluetoothSco.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mAudioStateManager.startBluetoothSco();
            }
        });

        Button buttonStopBluetoothSco = (Button) findViewById(R.id.buttonStopBluetoothSco);
        buttonStopBluetoothSco.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mAudioStateManager.stopBluetoothSco();
            }
        });

        //
        //
        //
        CompoundButton checkBoxSetBluetoothScoOn = (CompoundButton) findViewById(R.id.checkBoxSetBluetoothScoOn);
        checkBoxSetBluetoothScoOn.setOnCheckedChangeListener(new OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (mAudioStateManager.isBluetoothScoOn() != isChecked)
                {
                    mAudioStateManager.setBluetoothScoOn(isChecked);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean isBluetoothHeadsetConnected = mAudioStateManager.isBluetoothHeadsetConnected();
        boolean isBluetoothHeadsetAudioConnected = mAudioStateManager.isBluetoothHeadsetAudioConnected();

        MenuItem menuBluetoothHeadsetOn = menu.findItem(R.id.menu_bluetoothheadset_on);
        MenuItem menuBluetoothHeadsetOff = menu.findItem(R.id.menu_bluetoothheadset_off);
        MenuItem menuBluetoothHeadsetAudioOn = menu.findItem(R.id.menu_bluetoothheadsetaudio_on);
        MenuItem menuBluetoothHeadsetAudioOff = menu.findItem(R.id.menu_bluetoothheadsetaudio_off);

        menuBluetoothHeadsetOn.setVisible(isBluetoothHeadsetConnected);
        menuBluetoothHeadsetOff.setVisible(!isBluetoothHeadsetConnected);
        menuBluetoothHeadsetAudioOn.setVisible(isBluetoothHeadsetAudioConnected);
        menuBluetoothHeadsetAudioOff.setVisible(!isBluetoothHeadsetAudioConnected);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        updateScreen();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (mAudioStateManager != null)
        {
            mAudioStateManager.stop();
            mAudioStateManager = null;
        }
    }

    @Override
    public void onBackPressed()
    {
        Toast.makeText(this, "Exiting", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void handleMessage(Message msg)
    {
        if (VDBG)
        {
            Log.i(TAG, "handleMessage(" + msg + ")");
        }

        switch (msg.what)
        {
            case MSG_UPDATE_BLUETOOTH_INDICATION:
            {
                if (DBG)
                {
                    Log.d(TAG, "MSG_UPDATE_BLUETOOTH_INDICATION");
                }

                updateScreen();
                break;
            }

            case MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE:
            {
                if (DBG)
                {
                    Log.d(TAG, "MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE");
                }

                updateScreen();
                break;
            }
        }
    }

    private boolean mBluetoothHeadsetConnected = false;

    public void onBluetoothHeadsetConnected()
    {
        Log.i(TAG, "onBluetoothHeadsetConnected()");

        mBluetoothHeadsetConnected = true;

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

        mBluetoothHeadsetConnected = false;

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

    public void onAudioManagerScoAudioConnected()
    {
        Log.i(TAG, "onAudioManagerScoAudioConnected()");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();

        /*
        if (mPreviousAudioOutputAudioTrackStreamType != -1)
        {
            Log.e(TAG, "previousAudioOutputAudioTrackStreamType already set; headset reporting");
            return;
        }
        */

        // TODO:(pv) If speaker is playing, close and re-open speaker

        int streamType = AudioManager.STREAM_VOICE_CALL;

        mHandler //
        .obtainMessage(MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE) //
        .sendToTarget();
    }

    public void onAudioManagerScoAudioDisconnected(boolean error)
    {
        Log.i(TAG, "onAudioManagerScoAudioDisconnected(error=" + error + ")");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();

        // Force SCO off to try to repair what appears to be internal BT SCO state bugs in the OS.
        // Sometimes the only way to get SCO to connect is to reboot the phone and/or turn BT off and then back on.
        //mAudioStateManager.stopBluetoothSco();

        /*
        if (mBluetoothHeadsetConnected)
        {
            if (mAudioStateManager.isBluetoothScoAvailableOffCall())
            {
                mAudioStateManager.startBluetoothSco();
            }
        }
        */

        /*
        if (mPreviousAudioOutputAudioTrackStreamType == -1)
        {
            Log.e(TAG, "Not restoring previousAudioOutputAudioTrackStreamType because it is not set");
            return;
        }

        Log.e(TAG,
                        "Restoring previousAudioOutputAudioTrackStreamType="
                                        + AudioStateManager.audioOutputStreamTypeToString(mPreviousAudioOutputAudioTrackStreamType));
        int streamType = mPreviousAudioOutputAudioTrackStreamType;

        resetPreviousAudioOutputAudioTrackStreamType();
        */

        // TODO:(pv) If speaker is playing, close and re-open speaker

        mHandler //
        .obtainMessage(MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE) //
        .sendToTarget();
    }

    protected void updateScreen()
    {
        if (DBG)
        {
            Log.d(TAG, "updateScreen()...");
        }

        //boolean isBluetoothHeadsetConnected = audioStateManager.isBluetoothHeadsetConnected();
        //boolean isBluetoothHeadsetAudioConnected = audioStateManager.isBluetoothHeadsetAudioConnected();
        boolean isBluetoothScoOn = mAudioStateManager.isBluetoothScoOn();

        //
        //
        //
        invalidateOptionsMenu();

        //
        //
        //
        CompoundButton checkBoxSetBluetoothScoOn = (CompoundButton) findViewById(R.id.checkBoxSetBluetoothScoOn);
        checkBoxSetBluetoothScoOn.setChecked(isBluetoothScoOn);
    }
}
