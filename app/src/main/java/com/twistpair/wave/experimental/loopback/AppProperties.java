package com.twistpair.wave.experimental.loopback;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

public class AppProperties
{
    public static Bundle getMetaData(Context context)
    {
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getPackageName();
        ApplicationInfo applicationInfo;
        try
        {
            applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        }
        catch (NameNotFoundException e)
        {
            return null;
        }
        Bundle metaData = applicationInfo.metaData;
        return metaData;
    }

    public static String getMetaDataString(Context context, String key)
    {
        Bundle metaData = getMetaData(context);
        String value = metaData.getString(key);
        return value;
    }

    public static int getMetaDataInt(Context context, String key)
    {
        Bundle metaData = getMetaData(context);
        int value = metaData.getInt(key);
        return value;
    }
}
