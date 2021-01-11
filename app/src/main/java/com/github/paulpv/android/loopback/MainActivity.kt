package com.github.paulpv.android.loopback

import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import com.github.paulpv.android.loopback.FileBrowserDialog.FileBrowserDialogListener
import com.github.paulpv.android.loopback.audio.AudioStateManager
import com.github.paulpv.android.loopback.databinding.ActivityMainBinding
import java.util.*

//
// Audio ideas came from TPS code and:
//  http://android-er.blogspot.com/2012/06/audiorecord-and-audiotrack.html
//
class MainActivity //
//
    : FragmentActivity(), CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener, FileBrowserDialogListener //
{
    private lateinit var mApp: LoopbackApp
    private lateinit var mPreferences: LoopbackPreferences
    private lateinit var binding: ActivityMainBinding

    /*
    private var mCheckBoxSetBluetoothScoOn: CompoundButton
    private var mCheckBoxSetSpeakerphoneOn: CompoundButton
    private var mRadioButtonModeInCall: CompoundButton
    private var mRadioButtonModeInCommunication: CompoundButton
    private var mRadioButtonModeNormal: CompoundButton
    private var mRadioButtonModeRingtone: CompoundButton
    private var mRadioButtonSourceFile: RadioButton
    private var mTextViewSourceFile: TextView
    private var mRadioButtonSourceAudioRecord: RadioButton
    private var mRadioGroupSourceAudioRecord: RadioGroup
    private var mToggleButtonSource: CompoundButton
    private var mTextViewBufferCount: TextView
    private var mTextViewBufferPoolCount: TextView
    private var mRadioGroupOutputAudioTrack: RadioGroup
    private var mToggleButtonSpeaker: CompoundButton
    */

    /**
     * The audio sources radio buttons cannot be wrapped in a RadioGroup because they are split between layout views.
     * This collection is used to track the RadioButtons and determine which radio button is selected.
     */
    private val mAudioSources: MutableList<RadioButton?> = ArrayList()
    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        val audioInputFilePath = (findViewById<View>(R.id.textViewSourceFile) as TextView).text.toString()
        savedInstanceState.putString("audioInputFilePath", audioInputFilePath)

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mApp = application as LoopbackApp
        mPreferences = mApp.preferences
        val audioInputFilePath = if (savedInstanceState == null) {
            mPreferences.audioInputFilePath
        } else {
            savedInstanceState.getString("audioInputFilePath")
        }

        //
        //
        //
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.radioButtonModeInCall.setOnCheckedChangeListener(this)
        binding.radioButtonModeInCommunication.setOnCheckedChangeListener(this)
        binding.radioButtonModeNormal.setOnCheckedChangeListener(this)
        binding.radioButtonModeRingtone.setOnCheckedChangeListener(this)

        //
        //
        //
        val buttonStartBluetoothSco = findViewById<View>(R.id.buttonStartBluetoothSco) as Button
        buttonStartBluetoothSco.setOnClickListener { mApp.audioStateManager.startBluetoothSco() }
        val buttonStopBluetoothSco = findViewById<View>(R.id.buttonStopBluetoothSco) as Button
        buttonStopBluetoothSco.setOnClickListener { mApp.audioStateManager.stopBluetoothSco() }

        //
        //
        //

        binding.checkBoxSetBluetoothScoOn.setOnCheckedChangeListener(this)
        binding.checkBoxSetSpeakerphoneOn.setOnCheckedChangeListener(this)

        //
        //
        //

        val radioButtonSourceFile = binding.radioButtonSourceFile
        mAudioSources.add(radioButtonSourceFile)
        radioButtonSourceFile.setOnCheckedChangeListener(this)

        binding.textViewSourceFile.text = audioInputFilePath
        binding.textViewSourceFile.setOnClickListener { v ->
            // Pass in any existing path...
            val filePath = (v as TextView).text.toString()
            FileBrowserDialog.showDialog(this@MainActivity, filePath, AUDIO_FILE_ENDS_WITH)
        }

        binding.radioButtonSourceAudioRecordCamcorder.tag = MediaRecorder.AudioSource.CAMCORDER
        binding.radioButtonSourceAudioRecordDefault.tag = MediaRecorder.AudioSource.DEFAULT
        binding.radioButtonSourceAudioRecordMic.tag = MediaRecorder.AudioSource.MIC
        binding.radioButtonSourceAudioRecordVoiceCall.tag = MediaRecorder.AudioSource.VOICE_CALL
        binding.radioButtonSourceAudioRecordVoiceCommunication.tag = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        binding.radioButtonSourceAudioRecordVoiceDownlink.tag = MediaRecorder.AudioSource.VOICE_DOWNLINK
        binding.radioButtonSourceAudioRecordVoiceRecognition.tag = MediaRecorder.AudioSource.VOICE_RECOGNITION
        binding.radioButtonSourceAudioRecordVoiceUplink.tag = MediaRecorder.AudioSource.VOICE_UPLINK

        val radioButtonSourceAudioRecord = binding.radioButtonSourceAudioRecord
        mAudioSources.add(radioButtonSourceAudioRecord)
        radioButtonSourceAudioRecord.setOnCheckedChangeListener(this)
        radioButtonSourceAudioRecord.isChecked = true

        val prefAudioInputSourceType = mPreferences.audioInputAudioRecordSourceType
        Log.i(TAG, "onCreate: Selecting RadioButton w/ tag==prefAudioInputSourceType=" + AudioStateManager.audioInputAudioSourceToString(prefAudioInputSourceType))
        checkRadioButtonWithTag(binding.radioGroupSourceAudioRecord, prefAudioInputSourceType)

        binding.toggleButtonSource.setOnCheckedChangeListener(this)

        //
        //
        //

        binding.textViewBufferCount.setOnClickListener {
            // TODO:(pv) Prompt and clear mAudioBuffers...
            //mAudioBuffers.maintenance(true);
        }
        binding.textViewBufferPoolCount.setOnClickListener {
            // TODO:(pv) Prompt and clear mAudioBuffersPool...
            //mAudioBuffersPool.maintenance(true);
        }

        //
        //
        //

        binding.radioButtonOutputAudioTrackAlarm.tag = AudioManager.STREAM_ALARM
        binding.radioButtonOutputAudioTrackMusic.tag = AudioManager.STREAM_MUSIC
        binding.radioButtonOutputAudioTrackNotification.tag = AudioManager.STREAM_NOTIFICATION
        binding.radioButtonOutputAudioTrackRing.tag = AudioManager.STREAM_RING
        binding.radioButtonOutputAudioTrackSystem.tag = AudioManager.STREAM_SYSTEM
        binding.radioButtonOutputAudioTrackVoiceCall.tag = AudioManager.STREAM_VOICE_CALL

        val radioButtonOutputAudioTrack = binding.radioButtonOutputAudioTrack
        radioButtonOutputAudioTrack.setOnCheckedChangeListener(this)
        radioButtonOutputAudioTrack.isChecked = true

        val prefAudioOutputStreamType = mPreferences.audioOutputAudioTrackStreamType
        Log.i(TAG, "onCreate: Selecting RadioButton w/ tag==prefAudioOutputStreamType=" + AudioStateManager.audioOutputStreamTypeToString(prefAudioOutputStreamType))
        val radioGroupOutputAudioTrack = binding.radioGroupOutputAudioTrack
        radioGroupOutputAudioTrack.setOnCheckedChangeListener(this)
        checkRadioButtonWithTag(radioGroupOutputAudioTrack, prefAudioOutputStreamType)

        binding.toggleButtonSpeaker.setOnCheckedChangeListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val audioStateManager = mApp.audioStateManager
        val isBluetoothHeadsetConnected = audioStateManager.isBluetoothHeadsetConnected
        val isBluetoothHeadsetAudioConnected = audioStateManager.isBluetoothHeadsetAudioConnected
        val menuBluetoothHeadsetOn = menu.findItem(R.id.menu_bluetoothheadset_on)
        val menuBluetoothHeadsetOff = menu.findItem(R.id.menu_bluetoothheadset_off)
        val menuBluetoothHeadsetAudioOn = menu.findItem(R.id.menu_bluetoothheadsetaudio_on)
        val menuBluetoothHeadsetAudioOff = menu.findItem(R.id.menu_bluetoothheadsetaudio_off)
        menuBluetoothHeadsetOn.isVisible = isBluetoothHeadsetConnected
        menuBluetoothHeadsetOff.isVisible = !isBluetoothHeadsetConnected
        menuBluetoothHeadsetAudioOn.isVisible = isBluetoothHeadsetAudioConnected
        menuBluetoothHeadsetAudioOff.isVisible = !isBluetoothHeadsetAudioConnected
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        mApp.addForegroundContext(this)
        updateScreen()
    }

    override fun onPause() {
        super.onPause()
        mApp.removeForegroundContext(this)
    }

    override fun onDestroy() {
        // TODO stop threads?
        super.onDestroy()
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Exiting", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (val buttonViewId = buttonView.id) {
            R.id.radioButtonModeInCall -> if (isChecked) {
                Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_IN_CALL)")
                mApp.audioStateManager.mode = AudioManager.MODE_IN_CALL
            }
            R.id.radioButtonModeInCommunication -> if (isChecked) {
                Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION)")
                mApp.audioStateManager.mode = AudioManager.MODE_IN_COMMUNICATION
            }
            R.id.radioButtonModeNormal -> if (isChecked) {
                Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_NORMAL)")
                mApp.audioStateManager.mode = AudioManager.MODE_NORMAL
            }
            R.id.radioButtonModeRingtone -> if (isChecked) {
                Log.i(TAG, "mAudioManager.setMode(AudioManager.MODE_RINGTONE)")
                mApp.audioStateManager.mode = AudioManager.MODE_RINGTONE
            }
            R.id.checkBoxSetBluetoothScoOn -> {
                val audioStateManager = mApp.audioStateManager
                if (audioStateManager.isBluetoothScoOn != isChecked) {
                    audioStateManager.isBluetoothScoOn = isChecked
                }
            }
            R.id.checkBoxSetSpeakerphoneOn -> {
                val audioStateManager = mApp.audioStateManager
                if (audioStateManager.isSpeakerphoneOn != isChecked) {
                    audioStateManager.isSpeakerphoneOn = isChecked //, true);
                }
            }
            R.id.buttonStartBluetoothSco -> mApp.audioStateManager.startBluetoothSco()
            R.id.buttonStopBluetoothSco -> mApp.audioStateManager.stopBluetoothSco()
            R.id.radioButtonSourceFile, R.id.radioButtonSourceAudioRecord -> if (isChecked) {
                for (button in mAudioSources) {
                    if (button !== buttonView) {
                        button!!.isChecked = false
                    }
                }
                when (buttonViewId) {
                    R.id.radioButtonSourceFile -> {
                        enableRadioButtons(binding.radioGroupSourceAudioRecord, false)
                        val textViewSourceFile = findViewById<View>(R.id.textViewSourceFile) as TextView
                        val filePath = textViewSourceFile.text.toString()
                        // BUG:(pv) This is causing a browse when the orientation changes; need to handle saved instance logic
                        if (filePath.isEmpty()) {
                            FileBrowserDialog.showDialog(this, filePath, AUDIO_FILE_ENDS_WITH)
                            return
                        }
                        Log.i(TAG, "Audio source set to: File \"$filePath\"")
                    }
                    R.id.radioButtonSourceAudioRecord -> {
                        Log.i(TAG, "Audio source set to: Microphone")
                        enableRadioButtons(binding.radioGroupSourceAudioRecord, true)
                        val prefAudioInputSourceType = mPreferences.audioInputAudioRecordSourceType
                        Log.i(TAG, "onCheckedChanged: Selecting Input AudioRecord; selecting RadioButton w/ tag==prefAudioInputSourceType="
                                + AudioStateManager.audioInputAudioSourceToString(prefAudioInputSourceType))
                        checkRadioButtonWithTag(binding.radioGroupSourceAudioRecord, prefAudioInputSourceType)
                    }
                }
            }
            R.id.radioButtonOutputAudioTrack -> if (isChecked) {
                enableRadioButtons(binding.radioGroupOutputAudioTrack, true)
                val prefAudioOutputStreamType = mPreferences.audioOutputAudioTrackStreamType
                Log.i(TAG, "onCheckedChanged: Selecting Output AudioTrack; selecting RadioButton w/ tag==prefAudioOutputStreamType="
                        + AudioStateManager.audioOutputStreamTypeToString(prefAudioOutputStreamType))
                checkRadioButtonWithTag(binding.radioGroupOutputAudioTrack, prefAudioOutputStreamType)
            }
            R.id.toggleButtonSource -> {
                mApp.audioStopReadingMp3()
                mApp.audioStopReadingMicrophone()
                if (isChecked) {
                    val radioButtonSourceFile = findViewById<View>(R.id.radioButtonSourceFile) as RadioButton
                    val isSourceFile = radioButtonSourceFile.isChecked
                    val isSourceMicrophone = binding.radioButtonSourceAudioRecord.isChecked
                    if (isSourceFile) {
                        val textViewSourceFile = findViewById<View>(R.id.textViewSourceFile) as TextView
                        val audioInputFilePath = textViewSourceFile.text.toString()
                        mPreferences.putAudioInputFilePath(audioInputFilePath)
                        mApp.audioStartReadingMp3()
                    } else if (isSourceMicrophone) {
                        val checkedRadioButtonId = binding.radioGroupSourceAudioRecord.checkedRadioButtonId
                        val checkedRadioButton = findViewById<View>(checkedRadioButtonId) as RadioButton
                        val audioSource = (checkedRadioButton.tag as Int).toInt()
                        Log.i(TAG, "Audio input starting; saving preference audioSource="
                                + AudioStateManager.audioInputAudioSourceToString(audioSource))
                        mPreferences.putAudioInputAudioRecordSourceType(audioSource)
                        mApp.audioStartReadingMicrophone()
                    }
                }
            }
            R.id.toggleButtonSpeaker -> {
                mApp.audioStopPlayingSource()
                if (isChecked) {
                    val checkedRadioButtonId = binding.radioGroupOutputAudioTrack.checkedRadioButtonId
                    val checkedRadioButton = findViewById<View>(checkedRadioButtonId) as RadioButton
                    val streamType = (checkedRadioButton.tag as Int).toInt()
                    Log.i(TAG, "Audio output starting; saving preference streamType="
                            + AudioStateManager.audioOutputStreamTypeToString(streamType))
                    mPreferences.putAudioOutputAudioTrackStreamType(streamType)
                    mApp.audioStartPlayingSource()
                }
            }
        }
    }

    override fun onCheckedChanged(radioGroup: RadioGroup, checkedId: Int) {
        Log.i(TAG, "onCheckedChanged($radioGroup, $checkedId)")
        when (radioGroup.id) {
            R.id.radioGroupSourceAudioRecord -> {
                val audioSource = getRadioButtonTag(binding.radioGroupSourceAudioRecord, checkedId) as Int?
                mPreferences.putAudioInputAudioRecordSourceType(audioSource!!.toInt())
            }
            R.id.radioGroupOutputAudioTrack -> {
                //mApp.resetPreviousAudioOutputAudioTrackStreamType();
                val streamType = getRadioButtonTag(binding.radioGroupOutputAudioTrack, checkedId) as Int?
                mPreferences.putAudioOutputAudioTrackStreamType(streamType!!.toInt())
            }
        }
    }

    override fun onFinishFileSelected(path: String) {
        val textViewSourceFile = findViewById<View>(R.id.textViewSourceFile) as TextView
        textViewSourceFile.text = path
        mPreferences.putAudioInputFilePath(path)
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

    private fun updateScreen() {
        if (DBG) {
            Log.d(TAG, "updateScreen()...")
        }

        // Don't update anything if we're not in the foreground (there's
        // no point updating our UI widgets since we're not visible!)
        // Also note this check also ensures we won't update while we're
        // in the middle of pausing, which could cause a visible glitch in
        // the "activity ending" transition.
        if (!mApp.isForegroundContext(this)) {
            if (DBG) {
                Log.w(TAG, "updateScreen(): not in foreground; return;")
            }
            return
        }
        val audioStateManager = mApp.audioStateManager
        val audioMode = audioStateManager.mode
        //boolean isBluetoothHeadsetConnected = audioStateManager.isBluetoothHeadsetConnected();
        //boolean isBluetoothHeadsetAudioConnected = audioStateManager.isBluetoothHeadsetAudioConnected();
        val isBluetoothScoOn = audioStateManager.isBluetoothScoOn
        val isSpeakerphoneOn = audioStateManager.isSpeakerphoneOn

        //
        //
        //
        invalidateOptionsMenu()

        //
        //
        //
        binding.radioButtonModeInCall.isChecked = audioMode == AudioManager.MODE_IN_CALL
        binding.radioButtonModeInCommunication.isChecked = audioMode == AudioManager.MODE_IN_COMMUNICATION
        binding.radioButtonModeNormal.isChecked = audioMode == AudioManager.MODE_NORMAL
        binding.radioButtonModeRingtone.isChecked = audioMode == AudioManager.MODE_RINGTONE

        //
        //
        //
        //mCheckBoxSetBluetoothScoOn.setEnabled(isBluetoothHeadsetConnected);
        binding.checkBoxSetBluetoothScoOn.isChecked = isBluetoothScoOn
        binding.checkBoxSetSpeakerphoneOn.isChecked = isSpeakerphoneOn
    }

    fun handleMessage(msg: Message) {
        if (VDBG) {
            Log.i(TAG, "handleMessage($msg)")
        }
        when (msg.what) {
            LoopbackApp.MSG_UPDATE_BUFFER_COUNT -> {
                msg.target.removeMessages(LoopbackApp.MSG_UPDATE_BUFFER_COUNT)
                val audioBufferCount = msg.arg1
                val audioBufferPoolCount = msg.arg2
                binding.textViewBufferCount.text = audioBufferCount.toString()
                binding.textViewBufferPoolCount.text = audioBufferPoolCount.toString()
            }
            LoopbackApp.MSG_UPDATE_BLUETOOTH_INDICATION -> {
                if (DBG) {
                    Log.d(TAG, "REQUEST_UPDATE_BLUETOOTH_INDICATION")
                }

                // The bluetooth headset state changed, so some UI
                // elements may need to update.  (There's no need to
                // look up the current state here, since any UI
                // elements that care about the bluetooth state get it
                // directly from PhoneApp.showBluetoothIndication().)
                updateScreen()
            }
            LoopbackApp.MSG_UPDATE_AUDIO_OUTPUT_STREAM_TYPE -> {
                //val previousAudioOutputAudioTrackStreamType = mApp.previousAudioOutputAudioTrackStreamType
                val prefAudioOutputStreamType = mPreferences.audioOutputAudioTrackStreamType
                Log.i(TAG, "handleMessage: Selecting RadioButton w/ tag==prefAudioOutputStreamType=" + AudioStateManager.audioOutputStreamTypeToString(prefAudioOutputStreamType))
                checkRadioButtonWithTag(binding.radioGroupOutputAudioTrack, prefAudioOutputStreamType)
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val DBG = LoopbackApp.DBG_LEVEL >= 1
        private val VDBG = LoopbackApp.DBG_LEVEL >= 2
        private const val AUDIO_FILE_ENDS_WITH = ".mp3"

        private fun getCheckedRadioButtonTag(radioGroup: RadioGroup?): Any? {
            val checkedButtonId = radioGroup!!.checkedRadioButtonId
            return getRadioButtonTag(radioGroup, checkedButtonId)
        }

        private fun getRadioButtonTag(view: View?, radioButtonId: Int): Any? {
            if (radioButtonId != -1) {
                val radioButton = view!!.findViewById<View>(radioButtonId) as RadioButton
                return radioButton.tag
            }
            return null
        }

        private fun checkRadioButtonWithTag(radioGroup: RadioGroup?, tag: Any?) {
            requireNotNull(tag) { "tag cannot be null" }
            var radioButtonTag = getCheckedRadioButtonTag(radioGroup)
            if (radioButtonTag != null && tag == radioButtonTag) {
                Log.w(TAG, "radioButton w/ specified tag already checked; nothing to do; return;")
                return
            }
            val count = radioGroup!!.childCount
            for (i in 0 until count) {
                val radioButton = radioGroup.getChildAt(i) as RadioButton
                radioButtonTag = radioButton.tag
                if (tag == radioButtonTag) {
                    radioButton.isChecked = true
                    break
                }
            }
        }

        private fun enableRadioButtons(radioGroup: RadioGroup?, enabled: Boolean) {
            val count = radioGroup!!.childCount
            for (i in 0 until count) {
                val radioButton = radioGroup.getChildAt(i) as RadioButton
                radioButton.isEnabled = enabled
            }
        }
    }
}