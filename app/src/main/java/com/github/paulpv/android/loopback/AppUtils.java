package com.github.paulpv.android.loopback;

import java.util.Iterator;
import java.util.Set;

import android.os.Bundle;

public class AppUtils
{
    private AppUtils()
    {
    }

    public static String quote(Object value)
    {
        return (value == null) ? "null" : (value instanceof String) ? '\"' + value.toString() + '\"' : value.toString();
    }

    public static String toString(Bundle bundle)
    {
        StringBuilder sb = new StringBuilder();

        Set<String> keys = bundle.keySet();
        Iterator<String> it = keys.iterator();

        sb.append('{');
        while (it.hasNext())
        {
            String key = it.next();
            Object value = bundle.get(key);

            sb.append(quote(key)).append('=').append(quote(value));

            if (it.hasNext())
            {
                sb.append(", ");
            }
        }
        sb.append('}');

        return sb.toString();
    }
}
