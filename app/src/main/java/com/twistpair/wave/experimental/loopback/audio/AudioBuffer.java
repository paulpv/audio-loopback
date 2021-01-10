package com.twistpair.wave.experimental.loopback.audio;

/**
 * Handles the edge-case situation where the length of a previously allocated buffer differs from what is being requested.<br>
 * If the requested length is less than the previously allocated length, then marks the end of the buffer.
 * If the requested length is greater than the previously allocated length, then allocates a new buffer.   
 */
public class AudioBuffer
{
    private short[] buffer = null;
    private int     length = 0;

    AudioBuffer(int length)
    {
        setLength(length);
    }

    public short[] getBuffer()
    {
        return buffer;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        if (this.length < length)
        {
            // This should rarely/never happen; length should be constant
            //WtcLog.warn(TAG, "UNEXPECTED buffer.length < length; new AudioBuffer(length=" + length + ")");
            this.buffer = new short[length];
            this.length = length;
        }
    }
}
