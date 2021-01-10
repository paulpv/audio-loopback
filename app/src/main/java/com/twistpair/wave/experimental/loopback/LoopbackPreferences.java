package com.twistpair.wave.experimental.loopback;

import com.twistpair.wave.experimental.loopback.audio.AudioStateManager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Log;

// TODO:(pv) Use Backup API to save and get settings from Cloud
//  http://developer.android.com/training/cloudsync/backupapi.html
//  https://developers.google.com/android/backup/
public class LoopbackPreferences
{
    private static final String TAG                                    = LoopbackPreferences.class.getSimpleName();

    private static final String PERSONALITY                            = LoopbackApp.TAG;

    private static final String PREF_KEY_INPUT_FILE_PATH               = "audioInputFilePath";
    private static final String PREF_KEY_INPUT_AUDIORECORD_SOURCE_TYPE = "audioInputAudioRecordSourceType";
    private static final String PREF_KEY_OUTPUT_AUDIOTRACK_STREAM_TYPE = "audioOutputAudioTrackStreamType";

    private final LoopbackApp   mApp;

    public LoopbackPreferences(LoopbackApp app)
    {
        this.mApp = app;
    }

    public String getAudioInputFilePath()
    {
        String value = getString(mApp, PERSONALITY, PREF_KEY_INPUT_FILE_PATH, "");
        Log.i(TAG, "getAudioInputFilePath()=" + AppUtils.quote(value));
        return value;
    }

    public void putAudioInputFilePath(String value)
    {
        Log.i(TAG, "putAudioInputFilePath(" + AppUtils.quote(value) + ")");
        putString(mApp, PERSONALITY, PREF_KEY_INPUT_FILE_PATH, value);
    }

    public int getAudioInputAudioRecordSourceType()
    {
        int value = getInt(mApp, PERSONALITY, PREF_KEY_INPUT_AUDIORECORD_SOURCE_TYPE, MediaRecorder.AudioSource.MIC);
        Log.i(TAG, "getAudioInputAudioRecordSourceType()=" + AudioStateManager.audioInputAudioSourceToString(value));
        return value;
    }

    public void putAudioInputAudioRecordSourceType(int value)
    {
        Log.i(TAG, "putAudioInputAudioRecordSourceType(" + AudioStateManager.audioInputAudioSourceToString(value) + ")");
        putInt(mApp, PERSONALITY, PREF_KEY_INPUT_AUDIORECORD_SOURCE_TYPE, value);
    }

    public int getAudioOutputAudioTrackStreamType()
    {
        int value = getInt(mApp, PERSONALITY, PREF_KEY_OUTPUT_AUDIOTRACK_STREAM_TYPE, AudioManager.STREAM_MUSIC);
        Log.i(TAG, "getAudioOutputAudioTrackStreamType()=" + AudioStateManager.audioOutputStreamTypeToString(value));
        return value;
    }

    public void putAudioOutputAudioTrackStreamType(int value)
    {
        Log.i(TAG, "putAudioOutputAudioTrackStreamType(" + AudioStateManager.audioOutputStreamTypeToString(value) + ")");
        putInt(mApp, PERSONALITY, PREF_KEY_OUTPUT_AUDIOTRACK_STREAM_TYPE, value);
    }

    //
    //
    //

    private static SharedPreferences getPrivatePreferences(Context context, String personality)
    {
        return context.getSharedPreferences(personality, Activity.MODE_PRIVATE);
    }

    private static String getString(Context context, String personality, String key, String defValue)
    {
        return getPrivatePreferences(context, personality).getString(key, defValue);
    }

    private static void putString(Context context, String personality, String key, String value)
    {
        getPrivatePreferences(context, personality).edit() //
        .putString(key, value) //
        .commit();
    }

    private static boolean getBoolean(Context context, String personality, String key, boolean defValue)
    {
        return getPrivatePreferences(context, personality).getBoolean(key, defValue);
    }

    private static void putBoolean(Context context, String personality, String key, boolean value)
    {
        getPrivatePreferences(context, personality).edit() //
        .putBoolean(key, value) //
        .commit();
    }

    private static int getInt(Context context, String personality, String key, int defValue)
    {
        return getPrivatePreferences(context, personality).getInt(key, defValue);
    }

    private static void putInt(Context context, String personality, String key, int value)
    {
        getPrivatePreferences(context, personality).edit() //
        .putInt(key, value) //
        .commit();
    }
}
