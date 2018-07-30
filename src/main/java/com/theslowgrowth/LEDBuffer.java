package com.theslowgrowth;

public class LEDBuffer
{
    public LEDBuffer(int width, int height)
    {
        width_ = width;
        height_ = height;
        buffer_ = new Color[width][height];
        dirty_ = new boolean[width][height];
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                buffer_[x][y] = Color.OFF;
                dirty_[x][y] = false;
            }
        }
        globalDirty_ = false;
    }

    public boolean set(int x, int y, Color value)
    {
        if (x < 0)
            return false;
        if (x >= width_)
            return false;
        if (y < 0)
            return false;
        if (y >= height_)
            return false;

        if (buffer_[x][y] != value)
        {
            buffer_[x][y] = value;
            dirty_[x][y] = true;
            globalDirty_ = true;
            return true;
        }
        else
            return false;
    }

    public void flagDirty(int x, int y)
    {
        if (x < 0)
            return;
        if (x >= width_)
            return;
        if (y < 0)
            return;
        if (y >= height_)
            return;

        dirty_[x][y] = true;
        globalDirty_ = true;
    }

    public void flagDirty()
    {
        for (int x = 0; x < dirty_.length; x++)
        {
            for (int y = 0; y < dirty_[x].length; y++)
            {
                dirty_[x][y] = true;
            }
        }
        globalDirty_ = true;
    }

    public void flagClean()
    {
        for (int x = 0; x < dirty_.length; x++)
        {
            for (int y = 0; y < dirty_[x].length; y++)
            {
                dirty_[x][y] = false;
            }
        }
        globalDirty_ = false;
    }

    public boolean isDirty()
    {
        return globalDirty_;
    }

    public boolean isDirty(int x, int y)
    {
        if (x < 0)
            return false;
        if (x >= width_)
            return false;
        if (y < 0)
            return false;
        if (y >= height_)
            return false;

        return dirty_[x][y];
    }

    public Color get(int x, int y)
    {
        if (x < 0)
            return Color.OFF;
        if (x >= width_)
            return Color.OFF;
        if (y < 0)
            return Color.OFF;
        if (y >= height_)
            return Color.OFF;

        return buffer_[x][y];
    }

    public void mergeWith(LEDBuffer other)
    {
        int mx = Math.min(buffer_.length, other.buffer_.length);
        for (int x = 0; x < mx; x++)
        {
            int my = Math.min(buffer_[x].length, other.buffer_[x].length);
            for (int y = 0; y < my; y++)
            {
                if (buffer_[x][y] != other.buffer_[x][y])
                {
                    buffer_[x][y] = other.buffer_[x][y];
                    dirty_[x][y] = true;
                    globalDirty_ = true;
                }
            }
        }
    }

    public int getWidth() { return width_; }
    public int getHeight() { return height_; }

    private Color[][] buffer_;
    private boolean[][] dirty_;
    private boolean globalDirty_;
    private int width_;
    private int height_;
}
