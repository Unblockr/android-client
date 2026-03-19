package io.netbird.client;

import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.netbird.client.ui.egress.EgressGroup;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * HTTP client for the Netbird management REST API.
 * Used for egress group listing and peer group membership updates.
 */
public class NetbirdApiClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String EGRESS_GROUP_PREFIX = "egress-";

    private final String managementUrl;
    private final String token;
    private final OkHttpClient http;

    public NetbirdApiClient(String managementUrl, String token) {
        // Strip trailing slash; derive API base
        this.managementUrl = managementUrl.replaceAll("/+$", "");
        this.token = token;
        this.http = new OkHttpClient();
    }

    /** Returns groups whose names start with "egress-". */
    public List<EgressGroup> getEgressGroups() throws IOException {
        Request request = new Request.Builder()
                .url(managementUrl + "/api/groups")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GET /api/groups failed: " + response.code());
            }
            JSONArray array = new JSONArray(response.body().string());
            List<EgressGroup> result = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.optString("name", "");
                if (name.startsWith(EGRESS_GROUP_PREFIX)) {
                    String id = obj.optString("id", "");
                    int peersCount = obj.optInt("peers_count", 0);
                    result.add(new EgressGroup(id, name, peersCount));
                }
            }
            return result;
        } catch (Exception e) {
            throw new IOException("Failed to parse groups response", e);
        }
    }

    /**
     * Finds this device's peer ID by matching the device hostname against the peers list.
     * Returns null if not found.
     */
    public String getSelfPeerId() throws IOException {
        String hostname = Build.PRODUCT;
        Request request = new Request.Builder()
                .url(managementUrl + "/api/peers")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GET /api/peers failed: " + response.code());
            }
            JSONArray array = new JSONArray(response.body().string());
            for (int i = 0; i < array.length(); i++) {
                JSONObject peer = array.getJSONObject(i);
                String name = peer.optString("name", "");
                String peerHostname = peer.optString("hostname", "");
                if (hostname.equalsIgnoreCase(name) || hostname.equalsIgnoreCase(peerHostname)) {
                    return peer.optString("id", null);
                }
            }
            return null;
        } catch (Exception e) {
            throw new IOException("Failed to parse peers response", e);
        }
    }

    /**
     * Adds peerId to groupId's peers list.
     * Reads current group peers first to avoid overwriting other members.
     */
    public void addPeerToGroup(String peerId, String groupId) throws IOException {
        JSONObject group = getGroup(groupId);
        JSONArray peers = group.optJSONArray("peers");
        if (peers == null) peers = new JSONArray();

        // Check if already a member
        for (int i = 0; i < peers.length(); i++) {
            JSONObject p = peers.optJSONObject(i);
            if (p != null && peerId.equals(p.optString("id", ""))) {
                return; // Already in group
            }
        }

        // Build updated peers array (list of IDs for PUT body)
        JSONArray updatedPeerIds = new JSONArray();
        for (int i = 0; i < peers.length(); i++) {
            JSONObject p = peers.optJSONObject(i);
            if (p != null) updatedPeerIds.put(p.optString("id", ""));
        }
        updatedPeerIds.put(peerId);

        putGroup(groupId, group.optString("name", ""), updatedPeerIds);
    }

    /**
     * Removes peerId from groupId's peers list.
     */
    public void removePeerFromGroup(String peerId, String groupId) throws IOException {
        JSONObject group = getGroup(groupId);
        JSONArray peers = group.optJSONArray("peers");
        if (peers == null) return;

        JSONArray updatedPeerIds = new JSONArray();
        for (int i = 0; i < peers.length(); i++) {
            JSONObject p = peers.optJSONObject(i);
            if (p != null && !peerId.equals(p.optString("id", ""))) {
                updatedPeerIds.put(p.optString("id", ""));
            }
        }

        putGroup(groupId, group.optString("name", ""), updatedPeerIds);
    }

    private JSONObject getGroup(String groupId) throws IOException {
        Request request = new Request.Builder()
                .url(managementUrl + "/api/groups/" + groupId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("GET /api/groups/" + groupId + " failed: " + response.code());
            }
            return new JSONObject(response.body().string());
        } catch (Exception e) {
            throw new IOException("Failed to parse group response", e);
        }
    }

    private void putGroup(String groupId, String name, JSONArray peerIds) throws IOException {
        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("peers", peerIds);

            Request request = new Request.Builder()
                    .url(managementUrl + "/api/groups/" + groupId)
                    .header("Authorization", "Bearer " + token)
                    .put(RequestBody.create(body.toString(), JSON))
                    .build();

            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("PUT /api/groups/" + groupId + " failed: " + response.code());
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to build PUT request", e);
        }
    }
}
