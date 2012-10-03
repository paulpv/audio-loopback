package com.twistpair.wave.experimental.loopback;

import android.media.AudioManager;
import android.media.MediaRecorder;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.twistpair.wave.experimental.loopback.AudioStateManager.AudioStateListener;

public class MainActivity //
                extends //
                FragmentActivity //
                implements //
                CompoundButton.OnCheckedChangeListener, //
                RadioGroup.OnCheckedChangeListener, //
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
     * <li>msg.arg1: mAudioBuffers.size()</li>
     * <li>msg.arg2: mAudioBuffersPool.size()</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int      MSG_UPDATE_BUFFER_COUNT             = 1;

    /**
     * <ul>
     * <li>msg.arg1: unused</li>
     * <li>msg.arg2: unused</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int      MSG_UPDATE_BLUETOOTH_INDICATION     = 2;

    /**
     * <ul>
     * <li>msg.arg1: unused</li>
     * <li>msg.arg2: unused</li>
     * <li>msg.obj: unused</li>
     * </ul>
     */
    public static final int      MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE = 3;

    private Handler              mHandler;
    private AudioStateManager    mAudioStateManager;

    private CompoundButton       mCheckBoxSetBluetoothScoOn;
    private CompoundButton       mCheckBoxSetSpeakerphoneOn;
    private RadioGroup           mRadioGroupSourceAudioRecord;
    private CompoundButton       mToggleButtonSource;
    //private TextView                mTextViewBufferCount;
    //private TextView                mTextViewBufferPoolCount;
    private RadioGroup           mRadioGroupOutputAudioTrack;
    private CompoundButton       mToggleButtonSpeaker;

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
        mCheckBoxSetBluetoothScoOn = (CompoundButton) findViewById(R.id.checkBoxSetBluetoothScoOn);
        mCheckBoxSetBluetoothScoOn.setOnCheckedChangeListener(this);

        mCheckBoxSetSpeakerphoneOn = (CompoundButton) findViewById(R.id.checkBoxSetSpeakerphoneOn);
        mCheckBoxSetSpeakerphoneOn.setOnCheckedChangeListener(this);

        //
        //
        //
        RadioButton radioButtonInputAudioRecord = (RadioButton) findViewById(R.id.radioButtonSourceAudioRecord);

        mRadioGroupSourceAudioRecord = (RadioGroup) findViewById(R.id.radioGroupSourceAudioRecord);

        RadioButton radioButtonSourceAudioRecordCamcorder =
            (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordCamcorder);
        radioButtonSourceAudioRecordCamcorder.setTag(MediaRecorder.AudioSource.CAMCORDER);
        RadioButton radioButtonSourceAudioRecordDefault = (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordDefault);
        radioButtonSourceAudioRecordDefault.setTag(MediaRecorder.AudioSource.DEFAULT);
        RadioButton radioButtonSourceAudioRecordMic = (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordMic);
        radioButtonSourceAudioRecordMic.setTag(MediaRecorder.AudioSource.MIC);
        RadioButton radioButtonSourceAudioRecordVoiceCall =
            (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordVoiceCall);
        radioButtonSourceAudioRecordVoiceCall.setTag(MediaRecorder.AudioSource.VOICE_CALL);
        RadioButton radioButtonSourceAudioRecordVoiceCommunication =
            (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordVoiceCommunication);
        radioButtonSourceAudioRecordVoiceCommunication.setTag(MediaRecorder.AudioSource.VOICE_COMMUNICATION);
        RadioButton radioButtonSourceAudioRecordVoiceDownlink =
            (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordVoiceDownlink);
        radioButtonSourceAudioRecordVoiceDownlink.setTag(MediaRecorder.AudioSource.VOICE_DOWNLINK);
        RadioButton radioButtonSourceAudioRecordVoiceRecognition =
            (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordVoiceRecognition);
        radioButtonSourceAudioRecordVoiceRecognition.setTag(MediaRecorder.AudioSource.VOICE_RECOGNITION);
        RadioButton radioButtonSourceAudioRecordVoiceUplink =
            (RadioButton) findViewById(R.id.radioButtonSourceAudioRecordVoiceUplink);
        radioButtonSourceAudioRecordVoiceUplink.setTag(MediaRecorder.AudioSource.VOICE_UPLINK);

        radioButtonInputAudioRecord.setOnCheckedChangeListener(this);
        radioButtonInputAudioRecord.setChecked(true);

        mToggleButtonSource = (CompoundButton) findViewById(R.id.toggleButtonSource);
        mToggleButtonSource.setOnCheckedChangeListener(this);

        //
        //
        //
        /*
        mTextViewBufferCount = (TextView) findViewById(R.id.textViewBufferCount);
        mTextViewBufferCount.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                // TODO:(pv) Prompt and clear mAudioBuffers...
                //mAudioBuffers.maintenance(true);
            }
        });
        mTextViewBufferPoolCount = (TextView) findViewById(R.id.textViewBufferPoolCount);
        mTextViewBufferPoolCount.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                // TODO:(pv) Prompt and clear mAudioBuffersPool...
                //mAudioBuffersPool.maintenance(true);
            }
        });
        */

        //
        //
        //
        RadioButton radioButtonOutputAudioTrack = (RadioButton) findViewById(R.id.radioButtonOutputAudioTrack);

        mRadioGroupOutputAudioTrack = (RadioGroup) findViewById(R.id.radioGroupOutputAudioTrack);
        mRadioGroupOutputAudioTrack.setOnCheckedChangeListener(this);

        RadioButton radioButtonOutputAudioTrackAlarm = (RadioButton) findViewById(R.id.radioButtonOutputAudioTrackAlarm);
        radioButtonOutputAudioTrackAlarm.setTag(AudioManager.STREAM_ALARM);
        RadioButton radioButtonOutputAudioTrackMusic = (RadioButton) findViewById(R.id.radioButtonOutputAudioTrackMusic);
        radioButtonOutputAudioTrackMusic.setTag(AudioManager.STREAM_MUSIC);
        RadioButton radioButtonOutputAudioTrackNotification =
            (RadioButton) findViewById(R.id.radioButtonOutputAudioTrackNotification);
        radioButtonOutputAudioTrackNotification.setTag(AudioManager.STREAM_NOTIFICATION);
        RadioButton radioButtonOutputAudioTrackRing = (RadioButton) findViewById(R.id.radioButtonOutputAudioTrackRing);
        radioButtonOutputAudioTrackRing.setTag(AudioManager.STREAM_RING);
        RadioButton radioButtonOutputAudioTrackSystem = (RadioButton) findViewById(R.id.radioButtonOutputAudioTrackSystem);
        radioButtonOutputAudioTrackSystem.setTag(AudioManager.STREAM_SYSTEM);
        RadioButton radioButtonOutputAudioTrackVoiceCall =
            (RadioButton) findViewById(R.id.radioButtonOutputAudioTrackVoiceCall);
        radioButtonOutputAudioTrackVoiceCall.setTag(AudioManager.STREAM_VOICE_CALL);

        radioButtonOutputAudioTrack.setOnCheckedChangeListener(this);
        radioButtonOutputAudioTrack.setChecked(true);

        mToggleButtonSpeaker = (CompoundButton) findViewById(R.id.toggleButtonSpeaker);
        mToggleButtonSpeaker.setOnCheckedChangeListener(this);
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
    protected void onPause()
    {
        super.onPause();
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

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        int buttonViewId = buttonView.getId();
        switch (buttonViewId)
        {
            case R.id.checkBoxSetBluetoothScoOn:
            {
                if (mAudioStateManager.isBluetoothScoOn() != isChecked)
                {
                    mAudioStateManager.setBluetoothScoOn(isChecked);
                }
                break;
            }
            case R.id.checkBoxSetSpeakerphoneOn:
            {
                if (mAudioStateManager.isSpeakerphoneOn() != isChecked)
                {
                    mAudioStateManager.setSpeakerphoneOn(isChecked);
                }
                break;
            }

            case R.id.radioButtonSourceAudioRecord:
                if (isChecked)
                {
                }
                break;

            case R.id.radioButtonOutputAudioTrack:
                if (isChecked)
                {
                }
                break;

            //
            // TODO:(pv) Need to use AsyncTask here, otherwise rotation can cause the threads to continue running and later misbehave
            //
            case R.id.toggleButtonSource:
            {
                audioStopReadingMp3();
                audioStopReadingMicrophone();

                if (isChecked)
                {
                    int checkedRadioButtonId = mRadioGroupSourceAudioRecord.getCheckedRadioButtonId();
                    RadioButton checkedRadioButton = (RadioButton) findViewById(checkedRadioButtonId);
                    int audioSource = ((Integer) checkedRadioButton.getTag()).intValue();

                    audioStartReadingMicrophone();
                }

                break;
            }
            case R.id.toggleButtonSpeaker:
            {
                audioStopPlayingSource();

                if (isChecked)
                {
                    int checkedRadioButtonId = mRadioGroupOutputAudioTrack.getCheckedRadioButtonId();
                    RadioButton checkedRadioButton = (RadioButton) findViewById(checkedRadioButtonId);
                    int streamType = ((Integer) checkedRadioButton.getTag()).intValue();

                    audioStartPlayingSource();
                }
                break;
            }
        }
    }

    public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
    {
        Log.i(TAG, "onCheckedChanged(" + radioGroup + ", " + checkedId + ")");
        int radioGroupId = radioGroup.getId();
        switch (radioGroupId)
        {
            case R.id.radioGroupSourceAudioRecord:
            {
                Integer audioSource = (Integer) getRadioButtonTag(mRadioGroupSourceAudioRecord, checkedId);
                break;
            }
            case R.id.radioGroupOutputAudioTrack:
            {
                Integer streamType = (Integer) getRadioButtonTag(mRadioGroupOutputAudioTrack, checkedId);
                break;
            }
        }
    }

    private static Object getCheckedRadioButtonTag(RadioGroup radioGroup)
    {
        int checkedButtonId = radioGroup.getCheckedRadioButtonId();
        return getRadioButtonTag(radioGroup, checkedButtonId);
    }

    private static Object getRadioButtonTag(View view, int radioButtonId)
    {
        if (radioButtonId != -1)
        {
            RadioButton radioButton = (RadioButton) view.findViewById(radioButtonId);
            Object radioButtonTag = radioButton.getTag();
            return radioButtonTag;
        }
        return null;
    }

    private static void checkRadioButtonWithTag(RadioGroup radioGroup, Object tag)
    {
        if (tag == null)
        {
            throw new IllegalArgumentException("tag cannot be null");
        }

        Object radioButtonTag = getCheckedRadioButtonTag(radioGroup);
        if (radioButtonTag != null && tag.equals(radioButtonTag))
        {
            Log.w(TAG, "radioButton w/ specified tag already checked; nothing to do; return;");
            return;
        }

        int count = radioGroup.getChildCount();
        for (int i = 0; i < count; i++)
        {
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
            radioButtonTag = radioButton.getTag();
            if (tag.equals(radioButtonTag))
            {
                radioButton.setChecked(true);
                break;
            }
        }
    }

    private static void enableRadioButtons(RadioGroup radioGroup, boolean enabled)
    {
        int count = radioGroup.getChildCount();
        for (int i = 0; i < count; i++)
        {
            RadioButton radioButton = (RadioButton) radioGroup.getChildAt(i);
            radioButton.setEnabled(enabled);
        }
    }

    protected void updateScreen()
    {
        if (DBG)
        {
            Log.d(TAG, "updateScreen()...");
        }

        int audioMode = mAudioStateManager.getMode();
        //boolean isBluetoothHeadsetConnected = audioStateManager.isBluetoothHeadsetConnected();
        //boolean isBluetoothHeadsetAudioConnected = audioStateManager.isBluetoothHeadsetAudioConnected();
        boolean isBluetoothScoOn = mAudioStateManager.isBluetoothScoOn();
        boolean isSpeakerphoneOn = mAudioStateManager.isSpeakerphoneOn();

        //
        //
        //
        invalidateOptionsMenu();

        //
        //
        //
        mCheckBoxSetBluetoothScoOn.setChecked(isBluetoothScoOn);
        mCheckBoxSetSpeakerphoneOn.setChecked(isSpeakerphoneOn);
    }

    public void handleMessage(Message msg)
    {
        if (VDBG)
        {
            Log.i(TAG, "handleMessage(" + msg + ")");
        }

        switch (msg.what)
        {
            case MSG_UPDATE_BUFFER_COUNT:
            {
                msg.getTarget().removeMessages(MSG_UPDATE_BUFFER_COUNT);

                int audioBufferCount = msg.arg1;
                int audioBufferPoolCount = msg.arg2;
                //mTextViewBufferCount.setText(String.valueOf(audioBufferCount));
                //mTextViewBufferPoolCount.setText(String.valueOf(audioBufferPoolCount));
                break;
            }

            case MSG_UPDATE_BLUETOOTH_INDICATION:
            {
                if (DBG)
                {
                    Log.d(TAG, "REQUEST_UPDATE_BLUETOOTH_INDICATION");
                }

                // The bluetooth headset state changed, so some UI
                // elements may need to update.  (There's no need to
                // look up the current state here, since any UI
                // elements that care about the bluetooth state get it
                // directly from PhoneApp.showBluetoothIndication().)
                updateScreen();
                break;
            }

            case MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE:
            {
                break;
            }
        }
    }

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
    }

    public void onDockConnection(int state)
    {
        Log.i(TAG, "onDockConnection(" + state + ")");
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
        Log.i(TAG, "onAudioManagerScoAudioDisconnected()");
        mHandler //
        .obtainMessage(MSG_UPDATE_BLUETOOTH_INDICATION) //
        .sendToTarget();

        // Force SCO off to try to repair what appears to be internal BT SCO state bugs in the OS.
        // Sometimes the only way to get SCO to connect is to reboot the phone and/or turn BT off and then back on.
        mAudioStateManager.stopBluetoothSco();

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

    private void audioStartPlayingSource()
    {
        // TODO Auto-generated method stub
    }

    private void audioStopPlayingSource()
    {
        // TODO Auto-generated method stub
    }

    private void audioStartReadingMicrophone()
    {
        // TODO Auto-generated method stub
    }

    private void audioStopReadingMicrophone()
    {
        // TODO Auto-generated method stub
    }

    private void audioStopReadingMp3()
    {
        // TODO Auto-generated method stub
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
        /*
        int audioBufferCount = mAudioBuffers.size();
        int audioBufferPoolCount = mAudioBuffersPool.size();

        if (VDBG)
        {
            Log.i(TAG, "mAudioBuffers.size()=" + audioBufferCount + ", mAudioBuffersPool.size()=" + audioBufferPoolCount);
        }

        mHandler //
        .obtainMessage(MSG_UPDATE_BUFFER_COUNT, audioBufferCount, audioBufferPoolCount) //
        .sendToTarget();
        */
    }
}
