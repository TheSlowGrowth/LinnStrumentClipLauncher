package com.theslowgrowth;

import com.bitwig.extension.controller.api.Transport;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class BlinkTimer {
    BlinkTimer(LinnstrumentClipLauncherExtension parent, Transport transport, LEDBuffer buffer)
    {
        parent_ = parent;
        transport_ = transport;
        buffer_ = buffer;
        tasks_ = new LinkedList<BlinkTask>();
        internalTimerPosition_ = 0;
        active_ = true;
    }

    public void addTask(BlinkTask t) {
        tasks_.remove(t);
        tasks_.add(t);

        if (active_)
            startTimer();
    }

    public void removeTask(BlinkTask t) {
        tasks_.remove(t);
    }

    public void setActive(boolean shouldBeActive) {
        active_ = shouldBeActive;
        if (shouldBeActive)
            startTimer();
    }

    public void timerCallback()
    {
        double currentPosition;
        if (transport_.isPlaying().get())
        {
            currentPosition = transport_.getPosition().get();
        }
        else
        {
            internalTimerPosition_ += 0.125; // increment by a 32th note
            internalTimerPosition_ -= Math.floor(internalTimerPosition_);
            currentPosition = internalTimerPosition_;
        }

        if (tasks_.isEmpty())
            return;

        // queue the next timer cycle
        if (active_)
            startTimer();

        Iterator<BlinkTask> it = tasks_.iterator();
        while (it.hasNext()) {
            BlinkTask t = it.next();
            boolean isOn = false;
            switch (t.speed_) {
                case FULL:
                    isOn = (currentPosition % 4.0) < 2.0;
                    break;
                case HALF:
                    isOn = (currentPosition % 2.0) < 1.0;
                    break;
                case QUARTER:
                    isOn = (currentPosition % 1.0) < 0.5;
                    break;
                case _8TH:
                    isOn = (currentPosition % 0.5) < 0.25;
                    break;
                case _16TH:
                    isOn = (currentPosition % 0.25) < 0.125;
                    break;
            }
            buffer_.set(t.x_, t.y_, isOn?t.flashColor_:t.baseColor_);
        }
        parent_.getHost().requestFlush();
    }

    public void startTimer()
    {
        double bpm = transport_.tempo().value().get() * 646 + 20; // bitwig tempo goes from 20bpm to 666 bpm
        double currentPosition;
        if (transport_.isPlaying().get())
        {
            currentPosition = transport_.getPosition().get();
        }
        else
        {
            currentPosition = internalTimerPosition_;
        }

        double beatsUntilNextTick;
        beatsUntilNextTick = 1.0 - (8 * currentPosition - Math.floor(8 * currentPosition));
        beatsUntilNextTick /= 8.0;
        double timeInMillis = beatsUntilNextTick / bpm * 60 * 1000;
        parent_.getHost().scheduleTask(() -> timerCallback(), (long) timeInMillis);
    }

    enum BlinkSpeed { FULL, HALF, QUARTER, _8TH, _16TH }

    static public class BlinkTask {
        BlinkTask(int x, int y, Color baseColor, Color flashColor, BlinkSpeed speed)
        {
            x_ = x;
            y_ = y;
            baseColor_ = baseColor;
            flashColor_ = flashColor;
            speed_ = speed;
        }

        public boolean equals(Object o) {
            if (!(o instanceof BlinkTask)) {
                return false;
            }
            BlinkTask t = (BlinkTask) o;
            return (x_ == t.x_) && (y_ == t.y_);
        }

        final int x_;
        final int y_;
        final Color baseColor_;
        final Color flashColor_;
        final BlinkTimer.BlinkSpeed speed_;
    }

    private LinnstrumentClipLauncherExtension parent_;
    private Transport transport_;
    private LEDBuffer buffer_;
    private List<BlinkTask> tasks_;
    private boolean active_;
    private double internalTimerPosition_;
}
