package io.netbird.client.ui.egress;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import io.netbird.client.NetbirdApiClient;
import io.netbird.client.ui.PreferenceUI;

public class EgressViewModel extends ViewModel {

    public enum Status { LOADING, SUCCESS, ERROR, NO_AUTH }

    public static class UiState {
        public final Status status;
        public final List<EgressGroup> groups;
        public final String selectedGroupId;
        public final String errorMessage;

        UiState(Status status, List<EgressGroup> groups, String selectedGroupId, String errorMessage) {
            this.status = status;
            this.groups = groups;
            this.selectedGroupId = selectedGroupId;
            this.errorMessage = errorMessage;
        }
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>();

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void loadGroups(Context context) {
        String managementUrl = PreferenceUI.getManagementUrl(context);
        String token = PreferenceUI.getManagementToken(context);

        if (managementUrl == null || managementUrl.isEmpty() || token == null || token.isEmpty()) {
            uiState.postValue(new UiState(Status.NO_AUTH, null, null, null));
            return;
        }

        uiState.postValue(new UiState(Status.LOADING, null, null, null));

        String selectedGroupId = PreferenceUI.getSelectedEgressGroupId(context, managementUrl);
        NetbirdApiClient client = new NetbirdApiClient(managementUrl, token);

        new Thread(() -> {
            try {
                List<EgressGroup> groups = client.getEgressGroups();
                uiState.postValue(new UiState(Status.SUCCESS, groups, selectedGroupId, null));
            } catch (Exception e) {
                uiState.postValue(new UiState(Status.ERROR, null, null, e.getMessage()));
            }
        }).start();
    }

    public void selectGroup(Context context, EgressGroup group) {
        String managementUrl = PreferenceUI.getManagementUrl(context);
        String token = PreferenceUI.getManagementToken(context);
        String currentSelectedId = PreferenceUI.getSelectedEgressGroupId(context, managementUrl);
        NetbirdApiClient client = new NetbirdApiClient(managementUrl, token);

        new Thread(() -> {
            try {
                String peerId = PreferenceUI.getSelfPeerId(context, managementUrl);
                if (peerId == null) {
                    // Fetch and cache peer ID on first selection
                    peerId = client.getSelfPeerId();
                    if (peerId != null) {
                        PreferenceUI.setSelfPeerId(context, managementUrl, peerId);
                    }
                }

                if (peerId == null) {
                    uiState.postValue(new UiState(Status.ERROR, null, currentSelectedId,
                            "Could not identify this device as a peer"));
                    return;
                }

                // Remove from previous group if any
                if (currentSelectedId != null && !currentSelectedId.equals(group.id)) {
                    client.removePeerFromGroup(peerId, currentSelectedId);
                }

                // Add to new group
                client.addPeerToGroup(peerId, group.id);

                PreferenceUI.setSelectedEgressGroupId(context, managementUrl, group.id);
                loadGroups(context);
            } catch (Exception e) {
                uiState.postValue(new UiState(Status.ERROR, null, currentSelectedId, e.getMessage()));
            }
        }).start();
    }

    public void clearSelection(Context context) {
        String managementUrl = PreferenceUI.getManagementUrl(context);
        String token = PreferenceUI.getManagementToken(context);
        String currentSelectedId = PreferenceUI.getSelectedEgressGroupId(context, managementUrl);

        if (currentSelectedId == null || currentSelectedId.isEmpty()) {
            return;
        }

        NetbirdApiClient client = new NetbirdApiClient(managementUrl, token);

        new Thread(() -> {
            try {
                String peerId = PreferenceUI.getSelfPeerId(context, managementUrl);
                if (peerId == null) {
                    peerId = client.getSelfPeerId();
                    if (peerId != null) {
                        PreferenceUI.setSelfPeerId(context, managementUrl, peerId);
                    }
                }

                if (peerId != null) {
                    client.removePeerFromGroup(peerId, currentSelectedId);
                }

                PreferenceUI.setSelectedEgressGroupId(context, managementUrl, null);
                loadGroups(context);
            } catch (Exception e) {
                uiState.postValue(new UiState(Status.ERROR, null, currentSelectedId, e.getMessage()));
            }
        }).start();
    }
}
