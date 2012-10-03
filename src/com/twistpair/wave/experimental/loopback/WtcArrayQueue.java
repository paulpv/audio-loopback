package com.twistpair.wave.experimental.loopback;

import java.util.NoSuchElementException;

import android.util.Log;

/**
 * NOT THREAD SAFE: All methods must be synchronize by the caller as appropriate.<br>
 * Loosely based on http://developer.android.com/reference/java/util/Queue.html
 */
public class WtcArrayQueue
{
    private static final String TAG = WtcArrayQueue.class.getSimpleName();

    private final String        name;

    private Object[]            queue;
    private int                 head;
    private int                 tail;

    private int                 maxSizeCurrent;
    private int                 maxSizeLifetime;

    /**
     * NOT THREAD SAFE: All methods must be synchronize by the caller as appropriate.<br>
     * Loosely based on http://developer.android.com/reference/java/util/Queue.html
     * @param name The name to give this queue to report during maintenance
     */
    public WtcArrayQueue(String name)
    {
        this.name = name;
        clear();
    }

    public void maintenance(boolean clear)
    {
        if (clear)
        {
            clear();
        }
        else
        {
            Log.i(TAG, '$' + name + " maintenance: size=" + size() + ", capacity=" + capacity() //
                            + ", maxCurrent=" + maxSizeCurrent + ", maxLifetime=" + maxSizeLifetime);
        }
    }

    /**
     * Removes all elements from this queue, leaving it empty.
     */
    public void clear()
    {
        Log.d(TAG, '$' + name + " +clear(); size=" + size() + ", capacity=" + capacity() //
                        + ", maxCurrent=" + maxSizeCurrent + ", maxLifetime=" + maxSizeLifetime);
        queue = new Object[1];
        head = 0;
        tail = 0;
        maxSizeCurrent = 0;
        Log.d(TAG, '$' + name + " -clear(); size=" + size() + ", capacity=" + capacity() //
                        + ", maxCurrent=" + maxSizeCurrent + ", maxLifetime=" + maxSizeLifetime);
    }

    protected int capacity()
    {
        return (queue == null) ? 0 : queue.length;
    }

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this queue
     */
    public int size()
    {
        return (queue == null) ? 0 : (tail - head) & (queue.length - 1);
    }

    /**
     * Returns true if this queue contains no elements.
     * @return true if this queue contains no elements
     */
    public boolean isEmpty()
    {
        return tail == head;
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * @param element the element to add
     * @return true (always)
     */
    public boolean add(Object element)
    {
        if (element == null)
        {
            throw new IllegalArgumentException("element cannot be null");
        }

        //WtcLog.debug(TAG, '$' + name + " +add(e): size=" + size + ", capacity=" + queue.length);

        doubleCapacityIfNeeded();

        queue[tail] = element;
        tail = (tail + 1) % queue.length;

        int size = size();
        if (size > maxSizeCurrent)
        {
            maxSizeCurrent = size;

            if (maxSizeCurrent > maxSizeLifetime)
            {
                maxSizeLifetime = maxSizeCurrent;
            }
        }

        //WtcLog.debug(TAG, '$' + name + " -add(e): size=" + size + ", capacity=" + queue.length);
        return true;
    }

    /**
     * Retrieves and removes the head of this queue. This method throws an exception if this queue is empty.
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    public Object remove()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException(TAG + ": " + name + ".isEmpty() == true");
        }

        //WtcLog.debug(TAG, '$' + name + " +remove(): size=" + size() + ", capacity=" + queue.length);
        Object element = queue[head];
        head = (head + 1) % queue.length;
        //WtcLog.debug(TAG, '$' + name + " -remove(): size=" + size() + ", capacity=" + queue.length);
        return element;
    }

    private void doubleCapacityIfNeeded()
    {
        int next = (tail + 1) % queue.length;
        if (next == head)
        {
            int c = queue.length << 1;

            Object[] values = new Object[c];

            int size = size();
            for (int i = 0; i != size; i++)
            {
                values[i] = queue[(i + head) % queue.length];
            }

            queue = values;
            head = 0;
            tail = size;
        }
        //WtcLog.debug(TAG, '$' + name + " queue.length=" + queue.length);
    }
}