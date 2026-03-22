package io.netbird.client.ui.setup;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.UUID;

import io.netbird.client.tool.Preferences;
import io.netbird.client.tool.ProfileManagerWrapper;
import io.netbird.client.ui.PreferenceUI;
import io.netbird.gomobile.android.Android;
import io.netbird.gomobile.android.Auth;
import io.netbird.gomobile.android.ErrListener;

public class SetupViewModel extends ViewModel {

    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    public static class UiState {
        public final Status status;
        public final String errorMessage;

        UiState(Status status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }

    private final MutableLiveData<UiState> uiState =
            new MutableLiveData<>(new UiState(Status.IDLE, null));

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loginWithSetupKey(Context context, String setupKey) {
        if (!isValidSetupKey(setupKey)) {
            uiState.setValue(new UiState(Status.ERROR, "Invalid setup key format"));
            return;
        }

        uiState.setValue(new UiState(Status.LOADING, null));

        ProfileManagerWrapper profileManager = new ProfileManagerWrapper(context);
        String configFilePath;
        try {
            configFilePath = profileManager.getActiveConfigPath();
        } catch (Exception e) {
            configFilePath = context.getFilesDir() + "/config.json";
        }

        final String finalConfigPath = configFilePath;
        new Thread(() -> {
            try {
                Auth auth = Android.newAuth(finalConfigPath, Preferences.defaultServer());
                auth.loginWithSetupKeyAndSaveConfig(new ErrListener() {
                    @Override
                    public void onError(Exception e) {
                        uiState.postValue(new UiState(Status.ERROR, e.getMessage()));
                    }

                    @Override
                    public void onSuccess() {
                        PreferenceUI.setRegistered(context, true);
                        uiState.postValue(new UiState(Status.SUCCESS, null));
                    }
                }, setupKey, android.os.Build.PRODUCT);
            } catch (Exception e) {
                uiState.postValue(new UiState(Status.ERROR, e.getMessage()));
            }
        }).start();
    }

    public static boolean isValidSetupKey(String key) {
        if (key == null || key.length() != 36) return false;
        try {
            UUID.fromString(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
