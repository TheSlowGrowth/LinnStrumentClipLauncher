package com.theslowgrowth;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.callback.DoubleValueChangedCallback;
import com.bitwig.extension.callback.EnumValueChangedCallback;
import com.bitwig.extension.callback.ShortMidiMessageReceivedCallback;
import com.bitwig.extension.controller.api.*;
import com.bitwig.extension.controller.ControllerExtension;

public class LinnstrumentClipLauncherExtension extends ControllerExtension {
    LinnstrumentClipLauncherExtension(final LinnstrumentClipLauncherExtensionDefinition definition, final ControllerHost host) {
        super(definition, host);
    }

    public class DeviceChangedCallback implements EnumValueChangedCallback
    {
        public void valueChanged(String newValue) {
            // TODO: make script restart
            //host_.restart();
        }
    }
    public class MIDISettingsChangedCallback implements DoubleValueChangedCallback, EnumValueChangedCallback
    {
        public void valueChanged(double newValue) {
            int bend = (int) (bendRange_.get() * (BENDMAX - BENDMIN) + BENDMIN);
            noteInput_.setUseExpressiveMidi(true, Integer.parseInt(baseChannel_.get()) - 1, bend);
        }
        public void valueChanged(String newValue) {
            int bend = (int) (bendRange_.get() * (BENDMAX - BENDMIN) + BENDMIN);
            noteInput_.setUseExpressiveMidi(true, Integer.parseInt(baseChannel_.get()) - 1, bend);
        }
    }
    public class Button2CCChangedCallback implements DoubleValueChangedCallback
    {
        public void valueChanged(double newValue) {
            configureUserButton2((int) (newValue * (USERBUTTONCCMAX - USERBUTTONCCMIN) + USERBUTTONCCMIN));
        }
    }
    public class FinishOnChangeBackChangedCallback implements EnumValueChangedCallback
    {
        public void valueChanged(String newValue) {
            boolean finish = newValue.equals("Yes");
            clipLauncher_.setFinishRecOnShowPage(finish);
        }
    }

    @Override
    public void init() {

        // Early Init //////////////////////////////////////////
        host_ = getHost();
        host_.getMidiInPort(0).setMidiCallback((ShortMidiMessageReceivedCallback) msg -> onMidi0(msg));
        midiOut_ = host_.getMidiOutPort(0);
        noteInput_ = host_.getMidiInPort(0).createNoteInput("");

        // Load settings //////////////////////////////////////////

        // device setting
        SettableEnumValue device;
        device = host_.getPreferences().getEnumSetting("Size", "Hardware", new String[]{"Full", "128"}, "Full");
        int deviceWidth;
        if (device.get().equals("Full"))
            deviceWidth = 26;
        else
            deviceWidth = 17;
        // Base Channel
        baseChannel_ = host_.getPreferences().getEnumSetting("Base Channel", "Hardware", new String[]{"1", "16"}, "1");
        // Bend Range
        bendRange_ = host_.getPreferences().getNumberSetting("Bend range", "Hardware", BENDMIN, BENDMAX, 1, "semitones", DEFBEND);
        // User button 2 CC
        SettableRangedValue button2CC = host_.getPreferences().getNumberSetting("User Button 2 CC", "Hardware", USERBUTTONCCMIN, USERBUTTONCCMAX, 1, "", DEFUSERBUTTON2CC);
        configureUserButton2((int) (button2CC.get() * (USERBUTTONCCMAX - USERBUTTONCCMIN) + USERBUTTONCCMIN));
        // finish recording when changing back to clip launcher
        SettableEnumValue finishOnChangeBack = host_.getPreferences().getEnumSetting("Finish Rec when changing back to clip launcher", "Behaviour", new String[]{"Yes", "No"}, "Yes");

        // Late Init //////////////////////////

        buffer_ = new LEDBuffer(deviceWidth, 8);

        transport_ = host_.createTransport();
        transport_.isPlaying().markInterested();
        transport_.getPosition().markInterested();
        transport_.tempo().value().markInterested();

        host_.println("bend range" + (int) bendRange_.get() + ", " + bendRange_.get());
        noteInput_.setUseExpressiveMidi(true, Integer.parseInt(baseChannel_.get()) - 1, (int) bendRange_.get());
        noteInput_.setShouldConsumeEvents(false);

        sendInitializationMessages();

        boolean finish = finishOnChangeBack.get().equals("Yes");
        clipLauncher_ = new ClipLauncherPage(deviceWidth, 8, this, finish);

        changePage(null); // enter normal linnstrument mode

        // Add all observers
        finishOnChangeBack.addValueObserver( new FinishOnChangeBackChangedCallback());
        device.addValueObserver(new DeviceChangedCallback());
        baseChannel_.addValueObserver(new MIDISettingsChangedCallback());
        bendRange_.addValueObserver(new MIDISettingsChangedCallback());
        button2CC.addValueObserver(new Button2CCChangedCallback());

        // For now just show a popup notification for verification that it is running.
        host_.showPopupNotification("LinnStrument ClipLauncher started");
    }

    @Override
    public void exit() {
        // leave user firmware mode
        sendNRPN(245, 0);
        host_.showPopupNotification("LinnStrument ClipLauncher exited");
    }

    @Override
    public void flush() {
        if (page_ != null) {
            if (page_.getBuffer().isDirty())
            {
                //host_.println("merging dirty leds from page");
                buffer_.mergeWith(page_.getBuffer());
                page_.getBuffer().flagClean();
            }
        }
        updateDirtyLEDs();
    }

    private void sendNRPN(int NRPN, int value) {

        sendCC(99, NRPN >> 7);    // NRPN number MSB
        sendCC(98, NRPN & 0x7F);  // NRPN number LSB
        sendCC(6, value >> 7);    // NRPN value MSB
        sendCC(38, value & 0x7F); // NRPN value LSB
        sendCC(100, 127); // RPN parameter number Reset MSB
        sendCC(101, 127); // RPN parameter number Reset LSB
    }

    private void sendCC(int CC, int value) {
        midiOut_.sendMidi(0xB0, CC, value);
    }

    void changePage(LinnstrumentPage page) {
        if (page_ != null)
            page_.hide();


        if (page == null) {
            // disable leds that are still on
            LEDBuffer b = new LEDBuffer(buffer_.getWidth(), buffer_.getHeight());
            buffer_.mergeWith(b); // b has all LEDs set to OFF
            // send the now dirty flagged leds
            updateDirtyLEDs();

            enableNotePassthrough();
            // leave user firmware mode
            sendNRPN(245, 0);

            page_ = page;
            switchEnabled_ = false;
            host_.scheduleTask(() -> reEnableSwitching(), 400);
        } else {
            // re-enable the user mode if it was off before
            if (page_ == null)
            {
                // enter user firmware mode (manual control of LEDs,
                // switch off regular functionality)
                sendNRPN(245, 1);
                disableNotePassthrough();
            }

            switchEnabled_ = false;
            host_.scheduleTask(() -> reEnableSwitching(), 400);

            page_ = page;
            page_.show();
            buffer_.mergeWith(page_.getBuffer());
            page_.getBuffer().flagClean();
            host_.requestFlush(); // update LEDs soon
        }
    }

    public void enableNotePassthrough()
    {
        noteInput_.setKeyTranslationTable(PASSTHROUGHTABLE);
    }

    public void disableNotePassthrough()
    {
        noteInput_.setKeyTranslationTable(NOPASSTHROUGHTABLE);
    }

    private void updateDirtyLEDs()
    {
        if (buffer_.isDirty()) {
            for (int x = 0; x < buffer_.getWidth(); x++) {
                Boolean xSent = false;
                for (int y = 0; y < buffer_.getHeight(); y++) {
                    if (buffer_.isDirty(x, y)) {
                        if (!xSent)
                        {
                            xSent = true;
                            sendCC(20, (x & 0x7F));
                        }
                        sendCC(21, (7 - y) & 0x7F);
                        sendCC(22, buffer_.get(x, y).getValue());
                    }
                }
            }
            buffer_.flagClean();
        }
    }

    private void setLED(int x, int y, Color c)
    {
        sendCC(20, (x & 0x7F));
        sendCC(21, (7 - y) & 0x7F);
        sendCC(22, c.getValue());
    }

    private void configureUserButton2(int CC)
    {
        sendNRPN(229, 3); // make switch 2 send a midi CC message
        sendNRPN(258, CC); // set CC value
        userButton2CC_ = CC;
    }

    private void sendInitializationMessages()
    {
        // user switch 2 will be our toggle between the two modes
        configureUserButton2(16);

        ////////////////////////////////////////////////////////////////////////////
        // make global settings for the note mode (regular Linnstrument mode)
        sendNRPN(19, 24); // bend range: 24 semitones

        ////////////////////////////////////////////////////////////////////////////
        // make global settings for the clip launcher mode
        sendCC(13, 12); // set MIDI decimation rate for user firmware mode
    }

    /**
     * Called when we receive short MIDI message on port 0.
     */
    private void onMidi0(ShortMidiMessage msg) {
        if (processIncomingNRPN(msg))
            return;

        if (page_ != null) {
            // did the page consume this as a raw event?
            if (page_.onMidi(msg))
                return;

            // maybe it was a button event?
            if (msg.isNoteOn() || msg.isNoteOff())
            {
                int y = 7 - (msg.getStatusByte() & 0x0F); // coordinate transform: top-down
                int x = msg.getData1();
                int velo = msg.getData2();
                // filter user2 button presses to change the mode back to normal
                if ((x == 0) && (y == 5))
                {
                    if (msg.isNoteOn() && switchEnabled_)
                        changePage(null);
                    return;
                }
                if (msg.isNoteOn())
                    page_.buttonDown(x, y, velo);
                else
                    page_.buttonUp(x, y);
            }
        }
        else {
            // monitor the user button to switch modes
            if (msg.isControlChange()) {
                if (msg.getData1() == userButton2CC_) {
                    if (switchEnabled_)
                        changePage(clipLauncher_);
                }
            }
        }
    }

    private void onMidiNRPN(int NRPN, int value) {

    }

    private Boolean processIncomingNRPN(ShortMidiMessage msg) {
        if (msg.isControlChange()) {
            // number MSB
            if (msg.getData1() == 99) {
                currentNRPN_ &= 0x007F;
                currentNRPN_ |= msg.getData2() << 7;
                NRPNValueComplete_ = 0; // reset flags
                return true;
            }
            // number LSB
            else if (msg.getData1() == 98) {
                currentNRPN_ &= 0xFF80;
                currentNRPN_ |= msg.getData2() & 0x7F;
                NRPNValueComplete_ = 0; // reset flags
                return true;
            }
            // value ´MSB
            else if (msg.getData1() == 6) {
                currentNRPNValue_ &= 0x007F;
                currentNRPNValue_ |= msg.getData2() << 7;
                NRPNValueComplete_ |= 0x02;
                if (NRPNValueComplete_ == 0x03)
                    onMidiNRPN(currentNRPN_, currentNRPNValue_);
                return true;
            }
            // value LSB
            else if (msg.getData1() == 38) {
                currentNRPNValue_ &= 0xFF80;
                currentNRPNValue_ |= msg.getData2() & 0x7F;
                NRPNValueComplete_ |= 0x01;
                if (NRPNValueComplete_ == 0x03)
                    onMidiNRPN(currentNRPN_, currentNRPNValue_);
                return true;
            }
        }
        return false;
    }

    private void reEnableSwitching()
    {
        switchEnabled_ = true;
    }

    public Transport getTransport()
    {
        return transport_;
    }

    private NoteInput noteInput_;
    private Transport transport_;
    private LinnstrumentPage page_ = null;
    private LEDBuffer buffer_;
    private ControllerHost host_ = getHost();
    private MidiOut midiOut_;
    private ClipLauncherPage clipLauncher_;
    private int currentNRPN_ = 0;
    private int currentNRPNValue_ = 0;
    private int NRPNValueComplete_ = 0;
    private boolean switchEnabled_ = true;

    private SettableRangedValue bendRange_;
    private SettableEnumValue baseChannel_;
    private int userButton2CC_;

    private static final int BENDMIN = 1;
    private static final int BENDMAX = 48;
    private static final int DEFBEND = 24;
    private static final int USERBUTTONCCMIN = 1;
    private static final int USERBUTTONCCMAX = 127;
    private static final int DEFUSERBUTTON2CC = 16;
    private static final Integer[] PASSTHROUGHTABLE;
    private static final Integer[] NOPASSTHROUGHTABLE;
    static {
        PASSTHROUGHTABLE = new Integer[128];
        NOPASSTHROUGHTABLE = new Integer[128];
        for (int i = 0; i < 128; i++) {
            PASSTHROUGHTABLE[i] = i;
            NOPASSTHROUGHTABLE[i] = -1;
        }
    }
}
