package com.theslowgrowth;

import com.bitwig.extension.callback.ClipLauncherSlotBankPlaybackStateChangedCallback;
import com.bitwig.extension.callback.IndexedBooleanValueChangedCallback;
import com.bitwig.extension.callback.IndexedColorValueChangedCallback;
import com.bitwig.extension.controller.api.ClipLauncherSlotBank;
import com.bitwig.extension.controller.api.Track;
import com.bitwig.extension.controller.api.TrackBank;

public class ClipLauncherPage extends LinnstrumentPage {
    ClipLauncherPage(int width, int height, LinnstrumentClipLauncherExtension parent, boolean stopRecOnShowPage)
    {
        super(width, height, parent);

        timer_ = new BlinkTimer(parent, parent.getTransport(), getBuffer());

        numTracksVisible_ = width - 1 /* navigation column */- 1 /* scene column */ - 2 /* navigation columns */;
        numScenesVisible_ = height - 1 /* stop row */;
        stopRowY_ = height - 1;
        sceneLaunchX_ = width - 2;
        scrollColRightX_ = width - 1;

        duplicate_ = false;
        switchMode(Mode.LAUNCH);

        finishRecOnShowPage_ = stopRecOnShowPage;
        currentlyRecording_ = false;
        recordBank_ = parent.getHost().createMainTrackBank(1, 0, 1);
        recordBank_.scrollPosition().markInterested();
        recordBank_.sceneBank().scrollPosition().markInterested();

        clipStates_ = new ClipState[numTracksVisible_][numScenesVisible_];
        hasContent_ = new boolean[numTracksVisible_][numScenesVisible_];
        clipColors_ = new Color[numTracksVisible_][numScenesVisible_];
        trackBank_ = parent.getHost().createMainTrackBank(numTracksVisible_, 0, numScenesVisible_);
        trackBank_.scrollPosition().markInterested();
        trackBank_.sceneBank().scrollPosition().markInterested();
        for(int t = 0; t < numTracksVisible_; t++)
        {
            ClipLauncherSlotBank clipLauncher = trackBank_.getItemAt(t).clipLauncherSlotBank();
            clipLauncher.addHasContentObserver(new HasContentObserver(this, t));
            clipLauncher.addPlaybackStateObserver(new PlaybackStateObserver(this, t));
            clipLauncher.addColorObserver(new ColorObserver(this, t));
            for (int s = 0; s < numScenesVisible_; s++)
            {
                clipStates_[t][s] = new ClipState();
                hasContent_[t][s] = false;
                clipColors_[t][s] = Color.OFF;
            }
        }

        setLED(SCROLLCOLLEFTX, SCROLLUPBTTNY, Color.GREEN);
        setLED(SCROLLCOLLEFTX, SCROLLUPPAGEBTTNY, Color.GREEN);
        setLED(SCROLLCOLLEFTX, SCROLLDOWNBTTNY, Color.GREEN);
        setLED(SCROLLCOLLEFTX, SCROLLDOWNPAGEBTTNY, Color.GREEN);
        setLED(scrollColRightX_, SCROLLUPBTTNY, Color.GREEN);
        setLED(scrollColRightX_, SCROLLUPPAGEBTTNY, Color.GREEN);
        setLED(scrollColRightX_, SCROLLDOWNBTTNY, Color.GREEN);
        setLED(scrollColRightX_, SCROLLDOWNPAGEBTTNY, Color.GREEN);
        // set scroll left/right leds
        setLED(SCROLLCOLLEFTX, SCROLLHORBTTNY, Color.YELLOW);
        setLED(SCROLLCOLLEFTX, SCROLLHORPAGEBTTNY, Color.YELLOW);
        setLED(scrollColRightX_, SCROLLHORBTTNY, Color.YELLOW);
        setLED(scrollColRightX_, SCROLLHORPAGEBTTNY, Color.YELLOW);

        // set the scene LEDs
        for(int y = 0; y < numScenesVisible_; y++)
        {
            setLED(sceneLaunchX_, y, Color.CYAN);
        }

        // set the stop leds
        for (int x = CLIPSSTARTX; x <= sceneLaunchX_; x++)
            setLED(x, stopRowY_, Color.RED);
    }

    @Override
    protected void showImpl() {
        // Update indications in the app
        for(int p=0; p<numTracksVisible_; p++)
        {
            Track track = trackBank_.getItemAt(p);
            track.clipLauncherSlotBank().setIndication(true);
        }
        timer_.setActive(true);
        setDuplicateMode(false);

        // finish recording when this setting is enabled
        if ((finishRecOnShowPage_) && (currentlyRecording_))
        {
            // finish the recording
            recordBank_.getItemAt(0).clipLauncherSlotBank().launch(0);
            currentlyRecording_ = false;
        }
    }

    @Override
    protected void hideImpl() {
        // Update indications in the app
        for(int p=0; p<numTracksVisible_; p++)
        {
            Track track = trackBank_.getItemAt(p);
            track.clipLauncherSlotBank().setIndication(false);
        }
        timer_.setActive(false);
    }

    @Override
    public void buttonDown(int x, int y, int velocity) {
        // a cell on the menu column has been pressed
        if (x == 0)
        {
            if (y == RECORDBTTNY)
            {
                if (mode_ == Mode.RECORD)
                    switchMode(Mode.LAUNCH);
                else
                    switchMode(Mode.RECORD);
            }
            else if (y == DELETEBTTNY)
            {
                if (mode_ == Mode.DELETE)
                    switchMode(Mode.LAUNCH);
                else
                    switchMode(Mode.DELETE);
            }
            else if (y == DUPLICATEBTTNY)
            {
                setDuplicateMode(!duplicate_);
            }
        }
        // a cell in the scroll columns has been pressed
        else if ((x == SCROLLCOLLEFTX) || (x == scrollColRightX_))
        {
            if (y == SCROLLUPBTTNY)
                trackBank_.sceneBank().scrollBackwards();
            else if (y == SCROLLUPPAGEBTTNY)
                trackBank_.sceneBank().scrollPageBackwards();
            else if (y == SCROLLDOWNBTTNY)
                trackBank_.sceneBank().scrollForwards();
            else if (y == SCROLLDOWNPAGEBTTNY)
                trackBank_.sceneBank().scrollPageForwards();
            else if (y == SCROLLHORBTTNY)
            {
                if (x == scrollColRightX_)
                    trackBank_.scrollForwards();
                else
                    trackBank_.scrollBackwards();
            }
            else if (y == SCROLLHORPAGEBTTNY)
            {
                if (x == scrollColRightX_)
                    trackBank_.scrollPageForwards();
                else
                    trackBank_.scrollPageBackwards();
            }
        }
        // a cell in the scene launcher column has been pressed
        else if (x == sceneLaunchX_)
        {
            if (y == stopRowY_)
            {
                for(int t = 0; t < numTracksVisible_; t++)
                    trackBank_.getItemAt(t).clipLauncherSlotBank().stop();
            }
            else
            {
                if (duplicate_)
                {
                    // TODO: Duplicate scenes. How?!
                    // trackBank_.sceneBank().
                }
                else
                    trackBank_.sceneBank().launchScene(y);
            }

        }
        // a cell in the low row has been pressed (stop clips)
        else if (y == stopRowY_)
        {
            int cellX = x - CLIPSSTARTX;
            trackBank_.getItemAt(cellX).clipLauncherSlotBank().stop();
        }
        // a clip cell has been pressed
        else if ((x >= CLIPSSTARTX) && (x < CLIPSSTARTX + numTracksVisible_))
        {
            int cellX = x - CLIPSSTARTX;
            Track track = trackBank_.getItemAt(cellX);

            // duplicate mode //////////////////
            if (duplicate_) {
                track.clipLauncherSlotBank().duplicateClip(y);
                setDuplicateMode(false);
            }
            // record mode //////////////////
            else if (mode_ == Mode.RECORD)
            {
                ClipState state = clipStates_[cellX][y];
                // recording is queued, but its still playing or stopped right now
                if ((state.queuedState == ClipState.State.RECORDING)
                        && (state.currentState != ClipState.State.RECORDING))
                {
                    // revert back to previous state if recording was queued
                    switch (state.currentState)
                    {
                        case STOPPED:
                        case RECORDING:
                            track.clipLauncherSlotBank().stop();
                            break;
                        case PLAYING:
                            track.clipLauncherSlotBank().launch(y);
                            currentlyRecording_ = false;
                            break;
                    }
                }
                // already recording actively
                else if (state.currentState == ClipState.State.RECORDING)
                {
                    // launch if recording is done
                    track.clipLauncherSlotBank().launch(y);
                    currentlyRecording_ = false;
                }
                // now yet recording
                else
                {
                    // start recording
                    track.clipLauncherSlotBank().record(y);
                    // scroll the bank around, so it points to the right cell (so that we can later stop the recording,
                    // even if the main bank scrolled around in the meantime)
                    int t = recordBank_.scrollPosition().get();
                    int targetT = trackBank_.scrollPosition().get() + cellX;
                    int deltaT = targetT - t;
                    recordBank_.scrollBy(deltaT);
                    int s = recordBank_.sceneBank().scrollPosition().get();
                    int targetS = trackBank_.sceneBank().scrollPosition().get() + y;
                    int deltaS = targetS - s;
                    recordBank_.sceneBank().scrollBy(deltaS);
                    currentlyRecording_ = true;
                }
            }
            // delete mode /////////////////
            else if (mode_ == Mode.DELETE)
            {
                // TODO: long-press?
                track.clipLauncherSlotBank().deleteClip(y);
            }
            // launch mode /////////////////
            else
            {
                track.clipLauncherSlotBank().launch(y);
            }
        }
    }

    @Override
    public void buttonUp(int x, int y) {

    }

    void setFinishRecOnShowPage(boolean shouldStopRecOnShowPage)
    {
        finishRecOnShowPage_ = shouldStopRecOnShowPage;
    }

    private void setDuplicateMode(boolean shouldEnableDuplicating)
    {
        duplicate_ = shouldEnableDuplicating;
        if (duplicate_)
            timer_.addTask(new BlinkTimer.BlinkTask(0, DUPLICATEBTTNY, Color.OFF, Color.GREEN, BlinkTimer.BlinkSpeed._16TH));
        else {
            timer_.removeTask(new BlinkTimer.BlinkTask(0, DUPLICATEBTTNY, Color.OFF, Color.GREEN, BlinkTimer.BlinkSpeed._16TH));
            setLED(0, DUPLICATEBTTNY, Color.OFF);
        }
    }

    private void switchMode(Mode m) {
        mode_ = m;
        if (mode_ == Mode.LAUNCH)
        {
            setLED(0, RECORDBTTNY, Color.OFF);
            timer_.removeTask(new BlinkTimer.BlinkTask(0, DELETEBTTNY, Color.OFF, Color.RED, BlinkTimer.BlinkSpeed._16TH));
            setLED(0, DELETEBTTNY, Color.OFF);
        }
        else if (mode_== Mode.RECORD) {
            setLED(0, RECORDBTTNY, Color.RED);
            timer_.removeTask(new BlinkTimer.BlinkTask(0, DELETEBTTNY, Color.OFF, Color.RED, BlinkTimer.BlinkSpeed._16TH));
            setLED(0, DELETEBTTNY, Color.OFF);
        }
        else if (mode_== Mode.DELETE) {
            setLED(0, RECORDBTTNY, Color.OFF);
            timer_.addTask(new BlinkTimer.BlinkTask(0, DELETEBTTNY, Color.OFF, Color.RED, BlinkTimer.BlinkSpeed._16TH));
        }
    }

    private void updateClipLED(int track, int scene)
    {
        Color c = hasContent_[track][scene]?clipColors_[track][scene]:Color.OFF;
        setLED(track + CLIPSSTARTX, scene, c);


        ClipState state = clipStates_[track][scene];
        if (state.hasChangeQueued())
        {
            switch (state.queuedState)
            {
                case STOPPED:
                    timer_.addTask(new BlinkTimer.BlinkTask(track + CLIPSSTARTX, scene, c, Color.OFF, BlinkTimer.BlinkSpeed._16TH));
                    break;
                case PLAYING:
                    timer_.addTask(new BlinkTimer.BlinkTask(track + CLIPSSTARTX, scene, c, Color.WHITE, BlinkTimer.BlinkSpeed._16TH));
                    break;
                case RECORDING:
                    timer_.addTask(new BlinkTimer.BlinkTask(track + CLIPSSTARTX, scene, c, Color.RED, BlinkTimer.BlinkSpeed._16TH));
                    break;
            }
        }
        else
        {
            switch (state.queuedState)
            {
                case STOPPED:
                    timer_.removeTask(new BlinkTimer.BlinkTask(track + CLIPSSTARTX, scene, c, Color.OFF, BlinkTimer.BlinkSpeed._16TH));
                    break;
                case PLAYING:
                    timer_.addTask(new BlinkTimer.BlinkTask(track + CLIPSSTARTX, scene, c, Color.WHITE, BlinkTimer.BlinkSpeed.QUARTER));
                    break;
                case RECORDING:
                    timer_.addTask(new BlinkTimer.BlinkTask(track + CLIPSSTARTX, scene, c, Color.RED, BlinkTimer.BlinkSpeed.QUARTER));
                    break;
            }
        }
    }

    private class HasContentObserver implements IndexedBooleanValueChangedCallback
    {
        HasContentObserver(ClipLauncherPage parent, int track)
        {
            parent_ = parent;
            track_ = track;
        }
        public void valueChanged(int scene, boolean hasContent)
        {
            parent_.hasContent_[track_][scene] = hasContent;
            parent_.updateClipLED(track_, scene);
        }
        private ClipLauncherPage parent_;
        private int track_;
    }

    private class PlaybackStateObserver implements ClipLauncherSlotBankPlaybackStateChangedCallback
    {
        PlaybackStateObserver(ClipLauncherPage parent, int track)
        {
            parent_ = parent;
            track_ = track;
        }
        public void playbackStateChanged(int slotIndex, int playbackState, boolean isQueued) {

            if (isQueued) {
                switch (playbackState) {
                    case 0:
                        parent_.clipStates_[track_][slotIndex].queuedState = ClipState.State.STOPPED;
                        break;
                    case 1:
                        parent_.clipStates_[track_][slotIndex].queuedState = ClipState.State.PLAYING;
                        break;
                    case 2:
                        parent_.clipStates_[track_][slotIndex].queuedState = ClipState.State.RECORDING;
                        break;
                }
            } else {
                switch (playbackState) {
                    case 0:
                        parent_.clipStates_[track_][slotIndex].currentState = ClipState.State.STOPPED;
                        // bitwig doesn't send this
                        parent_.clipStates_[track_][slotIndex].queuedState = ClipState.State.STOPPED;
                        break;
                    case 1:
                        parent_.clipStates_[track_][slotIndex].currentState = ClipState.State.PLAYING;
                        // bitwig doesn't send this
                        parent_.clipStates_[track_][slotIndex].queuedState = ClipState.State.PLAYING;
                        break;
                    case 2:
                        parent_.clipStates_[track_][slotIndex].currentState = ClipState.State.RECORDING;
                        // bitwig doesn't send this
                        parent_.clipStates_[track_][slotIndex].queuedState = ClipState.State.RECORDING;
                        break;
                }
            }
            parent_.updateClipLED(track_, slotIndex);
        }
        private ClipLauncherPage parent_;
        private int track_;
    }

    private class ColorObserver implements IndexedColorValueChangedCallback
    {
        ColorObserver(ClipLauncherPage parent, int track)
        {
            parent_ = parent;
            track_ = track;
        }
        public void valueChanged(int scene, float red, float green, float blue)
        {
            Color c = Color.fromRGB(red, green, blue);
            parent_.clipColors_[track_][scene] = c;
            parent_.updateClipLED(track_, scene);
        }
        private ClipLauncherPage parent_;
        private int track_;
    }

    private TrackBank recordBank_; // used to be able to access cells into which is being recorded,
                                   // even if the main track bank scrolls around
    private TrackBank trackBank_;
    private BlinkTimer timer_;
    private ClipState[][] clipStates_;
    private boolean[][] hasContent_;
    private Color[][] clipColors_;
    private enum Mode { LAUNCH, RECORD, DELETE }
    private Mode mode_;
    private boolean duplicate_;
    private boolean finishRecOnShowPage_;
    private int numScenesVisible_;
    private int numTracksVisible_;
    private boolean currentlyRecording_;

    private static final int RECORDBTTNY = 0;
    private static final int DUPLICATEBTTNY = 1;
    private static final int DELETEBTTNY = 2;
    private static final int SCROLLUPBTTNY = 0;
    private static final int SCROLLUPPAGEBTTNY = 1;
    private static final int SCROLLDOWNBTTNY = 7;
    private static final int SCROLLDOWNPAGEBTTNY = 6;
    private static final int SCROLLHORBTTNY = 3;
    private static final int SCROLLHORPAGEBTTNY = 4;
    private static final int SCROLLCOLLEFTX = 1;
    private static int scrollColRightX_ = 0;
    private static int sceneLaunchX_ = 0;
    private static final int CLIPSSTARTX = 2;
    private static int stopRowY_ = 7;

}
