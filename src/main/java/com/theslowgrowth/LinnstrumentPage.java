package com.theslowgrowth;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;

public abstract class LinnstrumentPage {
    /**
     * @param width  width of the linnstrument surface
     * @param height height of the linnstrument surface
     * @param parent parent object
     */
    LinnstrumentPage(int width, int height, LinnstrumentClipLauncherExtension parent)
    {
        buffer_ = new LEDBuffer(width, height);
        parent_ = parent;
    }

    public void show()
    {
        showImpl();
    }

    public void hide()
    {
        hideImpl();
    }

    /**
     *  shows the page. Prepare to receive MIDI messages and do other initialization
     */
    protected abstract void showImpl();

    /**
     *  hides the page.
     */
    protected abstract void hideImpl();

    /**
     * @param msg   a midi message from the Linnstrument
     * @return  true, if the message was consumed
     */
    public Boolean onMidi(ShortMidiMessage msg) { return false; }

    public void buttonDown(int x, int y, int velocity) {}
    public void buttonUp(int x, int y) {}


    /**
     * sets an led
     * @param x
     * @param y
     * @param c
     */
    protected boolean setLED(int x, int y, Color c)
    {
        return buffer_.set(x, y, c);
    }

    public LEDBuffer getBuffer()
    {
        return buffer_;
    }

    public LinnstrumentClipLauncherExtension getParent()
    {
        return parent_;
    }

    private LinnstrumentClipLauncherExtension parent_;
    private LEDBuffer buffer_;
}
