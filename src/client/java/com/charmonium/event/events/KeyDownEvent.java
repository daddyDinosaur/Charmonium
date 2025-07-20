package com.charmonium.event.events;

import com.charmonium.event.listeners.AbstractListener;
import com.charmonium.event.listeners.KeyDownListener;

import java.util.ArrayList;

public class KeyDownEvent extends AbstractEvent {
    private final long window;
    private final int key;
    private final int scancode;
    private final int action;
    private final int modifiers;

    public KeyDownEvent(long window, int key, int scancode, int action, int modifiers) {
        this.window = window;
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    public long GetWindow() {
        return window;
    }

    public int GetKey() {
        return key;
    }

    public int GetScanCode() {
        return scancode;
    }

    public int GetAction() {
        return action;
    }

    public int GetModifiers() {
        return modifiers;
    }

    @Override
    public void Fire(ArrayList<? extends AbstractListener> listeners) {
        ArrayList<AbstractListener> listenersCopy = new ArrayList<>(listeners);
        for (AbstractListener listener : listenersCopy) {
            KeyDownListener keyDownListener = (KeyDownListener) listener;
            keyDownListener.onKeyDown(this);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<KeyDownListener> GetListenerClassType() {
        return KeyDownListener.class;
    }
}
