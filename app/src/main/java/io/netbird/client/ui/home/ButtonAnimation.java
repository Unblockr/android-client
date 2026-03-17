package io.netbird.client.ui.home;

import android.util.Log;
import android.widget.TextView;

class ButtonAnimation {
    private TextView textConnStatus;

    private enum AnimationState {
        DISCONNECTED("Disconnected"),
        CONNECTING("Connecting"),
        CONNECTED("Connected"),
        DISCONNECTING("Disconnecting");

        private final String text;
        AnimationState(String text) { this.text = text; }
    }

    private AnimationState currentState = AnimationState.DISCONNECTED;

    public ButtonAnimation() {
    }

    public void refresh(TextView textConnStatus) {
        Log.i("ButtonAnimation", "refresh: " + currentState);
        this.textConnStatus = textConnStatus;
        updateText(currentState.text);
    }

    public void destroy() {
        Log.d("ButtonAnimation", currentState + " -> destroy");
    }

    public void connecting() {
        if (currentState == AnimationState.CONNECTING) return;
        if (currentState == AnimationState.DISCONNECTING) return;
        currentState = AnimationState.CONNECTING;
        updateText(AnimationState.CONNECTING.text);
    }

    public void connected() {
        if (currentState == AnimationState.CONNECTED) return;
        currentState = AnimationState.CONNECTED;
        updateText(AnimationState.CONNECTED.text);
    }

    public void disconnecting() {
        if (currentState == AnimationState.DISCONNECTING) return;
        currentState = AnimationState.DISCONNECTING;
        updateText(AnimationState.DISCONNECTING.text);
    }

    public void disconnected() {
        if (currentState == AnimationState.DISCONNECTED) return;
        currentState = AnimationState.DISCONNECTED;
        updateText(AnimationState.DISCONNECTED.text);
    }

    private void updateText(String text) {
        Log.i("ButtonAnimation", "set text: " + text);
        textConnStatus.post(() -> textConnStatus.setText(text));
    }
}
