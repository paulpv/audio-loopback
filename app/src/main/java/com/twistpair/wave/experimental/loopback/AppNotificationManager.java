package com.twistpair.wave.experimental.loopback;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

/**
 * @author Paul
 * 
 * Ideas came from:
 * https://github.com/android/platform_packages_apps_phone/blob/master/src/com/android/phone/NotificationMgr.java
 */
public class AppNotificationManager
{
    private static final String           TAG = AppNotificationManager.class.getSimpleName();

    private static AppNotificationManager sInstance;

    private LoopbackApp                   mApp;
    //private Context             mContext;
    private NotificationManager           mNotificationManager;

    private AppNotificationManager(LoopbackApp app)
    {
        mApp = app;
        //mContext = app;
        mNotificationManager = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        //mStatusBarManager = (StatusBarManager) app.getSystemService(Context.STATUS_BAR_SERVICE);
        //mPowerManager = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        //mPhone = app.phone; // TODO: better style to use mCM.getDefaultPhone() everywhere instead
        //mCM = app.mCM;
        //statusBarHelper = new StatusBarHelper();
    }

    public static AppNotificationManager init(LoopbackApp app)
    {
        synchronized (AppNotificationManager.class)
        {
            if (sInstance == null)
            {
                sInstance = new AppNotificationManager(app);
                sInstance.updateNotificationsAtStartup();
            }
            else
            {
                Log.wtf(TAG, "init() called multiple times!  sInstance=" + sInstance);
            }
            return sInstance;
        }
    }

    private void updateNotificationsAtStartup()
    {
        // TODO Auto-generated method stub
    }

    public void updateSpeakerNotification(boolean flag)
    {
        // TODO Auto-generated method stub
    }
}
