package com.github.paulpv.android.loopback;

import java.util.NoSuchElementException;

/**
 * THREAD SAFE: All methods are internally synchronized.<br>
 * Loosely based on http://developer.android.com/reference/java/util/concurrent/ArrayBlockingQueue.html
 */
public class WtcArrayBlockingQueue
{
    private static final String TAG = WtcArrayBlockingQueue.class.getSimpleName();

    private final WtcArrayQueue queue;

    /**
     * THREAD SAFE: All methods are internally synchronized.<br>
     * Loosely based on http://developer.android.com/reference/java/util/concurrent/ArrayBlockingQueue.html
     * @param name The name to give this queue to report during maintenance
     */
    public WtcArrayBlockingQueue(String name)
    {
        queue = new WtcArrayQueue(name);
    }

    /**
     * DOES *NOT* CLEAR THE QUEUE IF THE QUEUE IS NOT EMPTY!<br>
     * The reason is because:
     * <ol>
     * <li>Other threads may be calling "add" indiscriminately.</li>
     * <li>Other threads may be calling "maintenance" indiscriminately.</li>
     * <li>Other threads may be occasionally blocking on "take" and then processing the element.</li>
     * </ol>
     * Thus, it would would be possible for "maintenance" to clear the queue after an "add" but
     * before the next "take", losing the possibly important elements in the queue.
     */
    public void maintenance()
    {
        synchronized (queue)
        {
            queue.maintenance(queue.isEmpty());
        }
    }

    public void clear()
    {
        synchronized (queue)
        {
            queue.clear();
        }
    }

    /**
     * Returns the number of elements in this queue.
     * @return the number of elements in this Queue
     */
    public int size()
    {
        synchronized (queue)
        {
            return queue.size();
        }
    }

    /**
     * Inserts the specified element at the tail of this queue.
     * @param element the element to add
     * @return true (always)
     */
    public boolean add(Object element)
    {
        /*
         * 2011/09/17: pv@twistpair.com
         * 
         * There is definitely a race-condition between "add" and "take"/"poll"/"maintenance".
         * 
         * The reason is described in the javadoc for Object.notify():
         * 
         *      Causes a thread which is waiting on this object's monitor (by means of calling one
         *      of the wait() methods) to be woken up. If more than one thread is waiting, one of
         *      them is chosen at the discretion of the virtual machine.
         *      
         *      The chosen thread will not run immediately. The thread that called notify() has to
         *      release the object's monitor first.
         *      
         *      Also, the chosen thread still has to compete against other threads that try to
         *      synchronize on the same object.
         *
         * Basically, when "add" releases its lock, any threads calling "take", "poll", or
         * "maintenance" will race to [re-]acquire their lock. If "poll" or "maintenance" wins then
         * the queue will empty and "take" will throw NoSuchElementException.
         * 
         * I have tried a few techniques to solve this, but none were simple, fast, or appeared to
         * fix the problem.
         * 
         * For now the following workarounds are in place:
         *  1) Conveniently, no callers call *both* "take" and "poll", so they currently don't conflict with each other.
         *  2) "maintenance" only clears the queue if it is empty.
         *  
         * Maybe one day I will actually getting around to properly fixing this code.
         * 
         */
        synchronized (queue)
        {
            queue.add(element);
            //WtcLog.info(TAG, "+queue.notify()");
            queue.notify();
            //WtcLog.info(TAG, "-queue.notify()");
            return true;
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
     * @return the head of this queue
     * @throws InterruptedException
     * @throws NoSuchElementException
     */
    public Object take() throws InterruptedException, NoSuchElementException
    {
        synchronized (queue)
        {
            if (queue.isEmpty())
            {
                //WtcLog.info(TAG, "+queue.wait()");
                queue.wait();
                //WtcLog.info(TAG, "-queue.wait()");
            }
            return queue.remove();
        }
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element to become available.
     * @param timeout milliseconds to wait before giving up, < 0 to wait indefinitely
     * @return the head of this queue, or null if the specified waiting time elapses before an element is available
     * @throws InterruptedException
     */
    public Object poll(long timeout) throws InterruptedException
    {
        if (timeout <= 0)
        {
            return take();
        }

        synchronized (queue)
        {
            if (queue.isEmpty())
            {
                //WtcLog.info(TAG, "+queue.wait(" + timeout + ')');
                queue.wait(timeout);
                //WtcLog.info(TAG, "-queue.wait(" + timeout + ')');
            }
            return queue.isEmpty() ? null : queue.remove();
        }
    }
}
