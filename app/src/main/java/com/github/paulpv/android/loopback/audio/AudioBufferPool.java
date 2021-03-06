package com.github.paulpv.android.loopback.audio;

import com.github.paulpv.android.loopback.MyArrayQueue;

public class AudioBufferPool
{
    private final MyArrayQueue queueAudioBufferPool = new MyArrayQueue("QueueAudioBufferPool");

    public void maintenance(boolean clear)
    {
        synchronized (queueAudioBufferPool)
        {
            queueAudioBufferPool.maintenance(clear);
        }
    }

    public int size()
    {
        synchronized (queueAudioBufferPool)
        {
            return queueAudioBufferPool.size();
        }
    }

    /**
     * Attempts to reuse memory from a queue of previously allocated memory; otherwise allocates new memory.
     * The intent is to save the CPU, memory, and garbage collection hit of perpetual allocations while streaming media.
     * @param length
     * @return
     */
    public AudioBuffer remove(int length)
    {
        synchronized (queueAudioBufferPool)
        {
            AudioBuffer buffer;

            //WtcLog.info(TAG, "BEFORE queueAudioBufferPool.size()=" + queueAudioBufferPool.size());
            if (queueAudioBufferPool.isEmpty())
            {
                //WtcLog.info(TAG, "new AudioBuffer(length=" + length + ")");
                buffer = new AudioBuffer(length);
            }
            else
            {
                //WtcLog.info(TAG, "queueAudioBufferPool.remove()");
                buffer = (AudioBuffer) queueAudioBufferPool.remove();
                buffer.setLength(length);
            }
            //WtcLog.info(TAG, "AFTER queueAudioBufferPool.size()=" + queueAudioBufferPool.size());

            return buffer;
        }
    }

    public void add(AudioBuffer buffer)
    {
        synchronized (queueAudioBufferPool)
        {
            //WtcLog.info(TAG, "queueAudioBufferPool.add(buffer)");
            queueAudioBufferPool.add(buffer);
        }
    }
}
