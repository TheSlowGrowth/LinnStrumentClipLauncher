package com.theslowgrowth;

public class ClipState {
    public enum State { STOPPED, PLAYING, RECORDING }

    public State currentState = State.STOPPED;
    public State queuedState = State.STOPPED;

    public Boolean hasChangeQueued() {
        return currentState != queuedState;
    }
}
