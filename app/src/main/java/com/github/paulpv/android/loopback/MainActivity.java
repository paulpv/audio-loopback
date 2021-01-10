package com.github.paulpv.android.loopback;

import java.util.ArrayList;
import java.util.List;

import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.github.paulpv.android.loopback.FileBrowserDialog.FileBrowserDialogListener;
import com.github.paulpv.android.loopback.audio.AudioStateManager;

//
// Audio ideas came from TPS code and:
//  http://android-er.blogspot.com/2012/06/audiorecord-and-audiotrack.html
//
public class MainActivity //
                extends //
                FragmentActivity //
                implements //
                CompoundButton.OnCheckedChangeListener, //
                RadioGroup.OnCheckedChangeListener, //
                FileBrowserDialogListener //
{
    private static final String     TAG                  = MainActivity.class.getSimpleName();

    private static final boolean    DBG                  = (LoopbackApp.DBG_LEVEL >= 1);
    private static final boolean    VDBG                 = (LoopbackApp.DBG_LEVEL >= 2);

    private static final String     AUDIO_FILE_ENDS_WITH = ".mp3";

    private LoopbackApp             mApp;
    private LoopbackPreferences     mPreferences;

    private CompoundButton          mCheckBoxSetBluetoothScoOn;
    private CompoundButton          mCheckBoxSetSpeakerphoneOn;
    private CompoundButton          mRadioButtonModeInCall;
    private CompoundButton          mRadioButtonModeInCommunication;
    private CompoundButton          mRadioButtonModeNormal;
    private CompoundButton          mRadioButtonModeRingtone;
    private RadioButton             mRadioButtonSourceFile;
    private TextView                mTextViewSourceFile;
    private RadioButton             mRadioButtonSourceAudioRecord;
    private RadioGroup              mRadioGroupSourceAudioRecord;
    private CompoundButton          mToggleButtonSource;
    private TextView                mTextViewBufferCount;
    private TextView                mTextViewBufferPoolCount;
    private RadioGroup              mRadioGroupOutputAudioTrack;
    private CompoundButton          mToggleButtonSpeaker;

    /**
     * The audio sources radio buttons cannot be wrapped in a RadioGroup because they are split between layout views.
     * This collection is used to track the RadioButtons and determine which radio button is selected.
     */
    private final List<RadioButton> mAudioSources        = new ArrayList<RadioButton>();

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState)
    {
        String audioInputFilePath = ((TextView) findViewById(R.id.textViewSourceFile)).getText().toString();

        savedInstanceState.putString("audioInputFilePath", audioInputFilePath);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApp = LoopbackApp.getInstance();
        mPreferences = mApp.getPreferences();

        final String audioInputFilePath;

        if (savedInstanceState == null)
        {
            audioInputFilePath = mPreferences.getAudioInputFilePath();
        }
        else
        {
            audioInputFilePath = savedInstanceState.getString("audioInputFilePath");
        }

        //
        //
        //
        mRadioButtonModeInCall = (CompoundButton) findViewById(R.id.radioButtonModeInCall);
        mRadioButtonModeInCall.setOnCheckedChangeListener(this);

        mRadioButtonModeInCommunication = (CompoundButton) findViewById(R.id.radioButtonModeInCommunication);
        mRadioButtonModeInCommunication.setOnCheckedChangeListener(this);

        mRadioButtonModeNormal = (CompoundButton) findViewById(R.id.radioButtonModeNormal);
        mRadioButtonModeNormal.setOnCheckedChangeListener(this);

        mRadioButtonModeRingtone = (CompoundButton) findViewById(R.id.radioButtonModeRingtone);
        mRadioButtonModeRingtone.setOnCheckedChangeListener(this);

        //
        //
        //
        Button buttonStartBluetoothSco = (Button) findViewById(R.id.buttonStartBluetoothSco);
        buttonStartBluetoothSco.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mApp.getAudioStateManager().startBluetoothSco();
            }
        });
        Button buttonStopBluetoothSco = (Button) findViewById(R.id.buttonStopBluetoothSco);
        buttonStopBluetoothSco.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                mApp.getAudioStateManager().stopBluetoothSco();
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
        mRadioButtonSourceFile = (RadioButton) findViewById(R.id.radioButtonSourceFile);
        mAudioSources.add(mRadioButtonSourceFile);
        mRadioButtonSourceFile.setOnCheckedChangeListener(this);

        mTextViewSourceFile = (TextView) findViewById(R.id.textViewSourceFile);
        mTextViewSourceFile.setText(audioInputFilePath);
        mTextViewSourceFile.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                // Pass in any existing path...
                String filePath = ((TextView) v).getText().toString();

                FileBrowserDialog.showDialog(MainActivity.this, filePath, AUDIO_FILE_ENDS_WITH);
            }
        });

        mRadioButtonSourceAudioRecord = (RadioButton) findViewById(R.id.radioButtonSourceAudioRecord);
        mAudioSources.add(mRadioButtonSourceAudioRecord);

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

        mRadioButtonSourceAudioRecord.setOnCheckedChangeListener(this);
        mRadioButtonSourceAudioRecord.setChecked(true);

        int prefAudioInputSourceType = mPreferences.getAudioInputAudioRecordSourceType();
        Log.i(TAG,
                        "onCreate: Selecting RadioButton w/ tag==prefAudioInputSourceType="
                                        + AudioStateManager.audioInputAudioSourceToString(prefAudioInputSourceType));
        checkRadioButtonWithTag(mRadioGroupSourceAudioRecord, prefAudioInputSourceType);

        mToggleButtonSource = (CompoundButton) findViewById(R.id.toggleButtonSource);
        mToggleButtonSource.setOnCheckedChangeListener(this);

        //
        //
        //
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

        int prefAudioOutputStreamType = mPreferences.getAudioOutputAudioTrackStreamType();
        Log.i(TAG,
                        "onCreate: Selecting RadioButton w/ tag==prefAudioOutputStreamType="
                                        + AudioStateManager.audioOutputStreamTypeToString(prefAudioOutputStreamType));
        checkRadioButtonWithTag(mRadioGroupOutputAudioTrack, prefAudioOutputStreamType);

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
        AudioStateManager audioStateManager = mApp.getAudioStateManager();
        boolean isBluetoothHeadsetConnected = audioStateManager.isBluetoothHeadsetConnected();
        boolean isBluetoothHeadsetAudioConnected = audioStateManager.isBluetoothHeadsetAudioConnected();

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
        mApp.addForegroundContext(this);
        updateScreen();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mApp.removeForegroundContext(this);
    }

    @Override
    protected void onDestroy()
    {
        // TODO stop threads?
        super.onDestroy();
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
            case R.id.radioButtonModeInCall:
                if (isChecked)
                {
                    Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_IN_CALL)");
                    mApp.getAudioStateManager().setMode(AudioManager.MODE_IN_CALL);
                }
                break;
            case R.id.radioButtonModeInCommunication:
                if (isChecked)
                {
                    Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION)");
                    mApp.getAudioStateManager().setMode(AudioManager.MODE_IN_COMMUNICATION);
                }
                break;
            case R.id.radioButtonModeNormal:
                if (isChecked)
                {
                    Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_NORMAL)");
                    mApp.getAudioStateManager().setMode(AudioManager.MODE_NORMAL);
                }
                break;
            case R.id.radioButtonModeRingtone:
                if (isChecked)
                {
                    Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_RINGTONE)");
                    mApp.getAudioStateManager().setMode(AudioManager.MODE_RINGTONE);
                }
                break;

            case R.id.checkBoxSetBluetoothScoOn:
            {
                AudioStateManager audioStateManager = mApp.getAudioStateManager();
                if (audioStateManager.isBluetoothScoOn() != isChecked)
                {
                    audioStateManager.setBluetoothScoOn(isChecked);
                }
                break;
            }
            case R.id.checkBoxSetSpeakerphoneOn:
            {
                AudioStateManager audioStateManager = mApp.getAudioStateManager();
                if (audioStateManager.isSpeakerphoneOn() != isChecked)
                {
                    audioStateManager.setSpeakerphoneOn(isChecked);//, true);
                }
                break;
            }

            case R.id.buttonStartBluetoothSco:
                mApp.getAudioStateManager().startBluetoothSco();
                break;
            case R.id.buttonStopBluetoothSco:
                mApp.getAudioStateManager().stopBluetoothSco();
                break;

            case R.id.radioButtonSourceFile:
            case R.id.radioButtonSourceAudioRecord:
                if (isChecked)
                {
                    for (RadioButton button : mAudioSources)
                    {
                        if (button != buttonView)
                        {
                            button.setChecked(false);
                        }
                    }

                    switch (buttonViewId)
                    {
                        case R.id.radioButtonSourceFile:
                        {
                            enableRadioButtons(mRadioGroupSourceAudioRecord, false);

                            TextView textViewSourceFile = (TextView) findViewById(R.id.textViewSourceFile);
                            String filePath = textViewSourceFile.getText().toString();
                            // BUG:(pv) This is causing a browse when the orientation changes; need to handle saved instance logic
                            if (filePath.length() == 0)
                            {
                                FileBrowserDialog.showDialog(this, filePath, AUDIO_FILE_ENDS_WITH);
                                return;
                            }

                            Log.i(TAG, "Audio source set to: File \"" + filePath + "\"");
                            break;
                        }
                        case R.id.radioButtonSourceAudioRecord:
                        {
                            Log.i(TAG, "Audio source set to: Microphone");

                            enableRadioButtons(mRadioGroupSourceAudioRecord, true);

                            int prefAudioInputSourceType = mPreferences.getAudioInputAudioRecordSourceType();
                            Log.i(TAG,
                                            "onCheckedChanged: Selecting Input AudioRecord; selecting RadioButton w/ tag==prefAudioInputSourceType="
                                                            + AudioStateManager.audioInputAudioSourceToString(prefAudioInputSourceType));
                            checkRadioButtonWithTag(mRadioGroupSourceAudioRecord, prefAudioInputSourceType);

                            break;
                        }
                    }
                }
                break;

            case R.id.radioButtonOutputAudioTrack:
                if (isChecked)
                {
                    enableRadioButtons(mRadioGroupOutputAudioTrack, true);

                    int prefAudioOutputStreamType = mPreferences.getAudioOutputAudioTrackStreamType();
                    Log.i(TAG,
                                    "onCheckedChanged: Selecting Output AudioTrack; selecting RadioButton w/ tag==prefAudioOutputStreamType="
                                                    + AudioStateManager.audioOutputStreamTypeToString(prefAudioOutputStreamType));
                    checkRadioButtonWithTag(mRadioGroupOutputAudioTrack, prefAudioOutputStreamType);
                }
                break;

            //
            // TODO:(pv) Need to use AsyncTask here, otherwise rotation can cause the threads to continue running and later misbehave
            //
            case R.id.toggleButtonSource:
            {
                mApp.audioStopReadingMp3();
                mApp.audioStopReadingMicrophone();

                if (isChecked)
                {
                    RadioButton radioButtonSourceFile = (RadioButton) findViewById(R.id.radioButtonSourceFile);

                    boolean isSourceFile = radioButtonSourceFile.isChecked();
                    boolean isSourceMicrophone = mRadioButtonSourceAudioRecord.isChecked();

                    if (isSourceFile)
                    {
                        TextView textViewSourceFile = (TextView) findViewById(R.id.textViewSourceFile);
                        String audioInputFilePath = textViewSourceFile.getText().toString();

                        mPreferences.putAudioInputFilePath(audioInputFilePath);

                        mApp.audioStartReadingMp3();
                    }
                    else if (isSourceMicrophone)
                    {
                        int checkedRadioButtonId = mRadioGroupSourceAudioRecord.getCheckedRadioButtonId();
                        RadioButton checkedRadioButton = (RadioButton) findViewById(checkedRadioButtonId);
                        int audioSource = ((Integer) checkedRadioButton.getTag()).intValue();

                        Log.i(TAG,
                                        "Audio input starting; saving preference audioSource="
                                                        + AudioStateManager.audioInputAudioSourceToString(audioSource));
                        mPreferences.putAudioInputAudioRecordSourceType(audioSource);

                        mApp.audioStartReadingMicrophone();
                    }
                }

                break;
            }
            case R.id.toggleButtonSpeaker:
            {
                mApp.audioStopPlayingSource();
                if (isChecked)
                {
                    int checkedRadioButtonId = mRadioGroupOutputAudioTrack.getCheckedRadioButtonId();
                    RadioButton checkedRadioButton = (RadioButton) findViewById(checkedRadioButtonId);
                    int streamType = ((Integer) checkedRadioButton.getTag()).intValue();

                    Log.i(TAG,
                                    "Audio output starting; saving preference streamType="
                                                    + AudioStateManager.audioOutputStreamTypeToString(streamType));
                    mPreferences.putAudioOutputAudioTrackStreamType(streamType);

                    mApp.audioStartPlayingSource();
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
                mPreferences.putAudioInputAudioRecordSourceType(audioSource.intValue());
                break;
            }
            case R.id.radioGroupOutputAudioTrack:
            {
                //mApp.resetPreviousAudioOutputAudioTrackStreamType();

                Integer streamType = (Integer) getRadioButtonTag(mRadioGroupOutputAudioTrack, checkedId);
                mPreferences.putAudioOutputAudioTrackStreamType(streamType.intValue());
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

    public void onFinishFileSelected(String path)
    {
        TextView textViewSourceFile = (TextView) findViewById(R.id.textViewSourceFile);
        textViewSourceFile.setText(path);
        mPreferences.putAudioInputFilePath(path);
    }

    /*
    public void requestUpdateBluetoothIndication()
    {
        if (VDBG)
        {
            Log.d(TAG, "requestUpdateBluetoothIndication()...");
        }

        // No need to look at the current state here; any UI elements that
        // care about the bluetooth state (i.e. the CallCard) get
        // the necessary state directly from PhoneApp.showBluetoothIndication().
        //mHandler.removeMessages(REQUEST_UPDATE_BLUETOOTH_INDICATION);
        //mHandler.sendEmptyMessage(REQUEST_UPDATE_BLUETOOTH_INDICATION);
    }
    */

    protected void updateScreen()
    {
        if (DBG)
        {
            Log.d(TAG, "updateScreen()...");
        }

        // Don't update anything if we're not in the foreground (there's
        // no point updating our UI widgets since we're not visible!)
        // Also note this check also ensures we won't update while we're
        // in the middle of pausing, which could cause a visible glitch in
        // the "activity ending" transition.
        if (!mApp.isForegroundContext(this))
        {
            if (DBG)
            {
                Log.w(TAG, "updateScreen(): not in foreground; return;");
            }
            return;
        }

        AudioStateManager audioStateManager = mApp.getAudioStateManager();
        int audioMode = audioStateManager.getMode();
        //boolean isBluetoothHeadsetConnected = audioStateManager.isBluetoothHeadsetConnected();
        //boolean isBluetoothHeadsetAudioConnected = audioStateManager.isBluetoothHeadsetAudioConnected();
        boolean isBluetoothScoOn = audioStateManager.isBluetoothScoOn();
        boolean isSpeakerphoneOn = audioStateManager.isSpeakerphoneOn();

        //
        //
        //
        invalidateOptionsMenu();

        //
        //
        //
        mRadioButtonModeInCall.setChecked(audioMode == AudioManager.MODE_IN_CALL);
        mRadioButtonModeInCommunication.setChecked(audioMode == AudioManager.MODE_IN_COMMUNICATION);
        mRadioButtonModeNormal.setChecked(audioMode == AudioManager.MODE_NORMAL);
        mRadioButtonModeRingtone.setChecked(audioMode == AudioManager.MODE_RINGTONE);

        //
        //
        //
        //mCheckBoxSetBluetoothScoOn.setEnabled(isBluetoothHeadsetConnected);
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
            case LoopbackApp.MSG_UPDATE_BUFFER_COUNT:
            {
                msg.getTarget().removeMessages(LoopbackApp.MSG_UPDATE_BUFFER_COUNT);

                int audioBufferCount = msg.arg1;
                int audioBufferPoolCount = msg.arg2;
                mTextViewBufferCount.setText(String.valueOf(audioBufferCount));
                mTextViewBufferPoolCount.setText(String.valueOf(audioBufferPoolCount));
                break;
            }

            case LoopbackApp.MSG_UPDATE_BLUETOOTH_INDICATION:
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

            case LoopbackApp.MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE:
            {
                int previousAudioOutputAudioTrackStreamType = mApp.getPreviousAudioOutputAudioTrackStreamType();

                int prefAudioOutputStreamType = mPreferences.getAudioOutputAudioTrackStreamType();
                Log.i(TAG,
                                "handleMessage: Selecting RadioButton w/ tag==prefAudioOutputStreamType="
                                                + AudioStateManager.audioOutputStreamTypeToString(prefAudioOutputStreamType));
                checkRadioButtonWithTag(mRadioGroupOutputAudioTrack, prefAudioOutputStreamType);
                break;
            }
        }
    }
}
