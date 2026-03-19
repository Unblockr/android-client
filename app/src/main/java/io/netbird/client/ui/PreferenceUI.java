package io.netbird.client.ui;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUI {
    private static final String PREFS_NAME = "ui_prefs";
    private static final String FIRST_LAUNCH_KEY = "first_launch";
    private static final String REGISTERED_KEY = "registered";
    private static final String MANAGEMENT_URL_KEY = "management_url";
    private static final String MANAGEMENT_TOKEN_KEY = "management_token";
    private static final String EGRESS_GROUP_KEY_PREFIX = "egress_group_";
    private static final String SELF_PEER_ID_KEY_PREFIX = "self_peer_id_";

    public static boolean isFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(FIRST_LAUNCH_KEY, true);
    }

    public static void setFirstLaunchDone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(FIRST_LAUNCH_KEY, false).apply();
    }

    public static boolean isRegistered(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(REGISTERED_KEY, false);
    }

    public static void setRegistered(Context context, boolean registered) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(REGISTERED_KEY, registered).apply();
    }

    public static String getManagementUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(MANAGEMENT_URL_KEY, null);
    }

    public static void setManagementUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(MANAGEMENT_URL_KEY, url).apply();
    }

    public static String getManagementToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(MANAGEMENT_TOKEN_KEY, null);
    }

    public static void setManagementToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(MANAGEMENT_TOKEN_KEY, token).apply();
    }

    public static String getSelectedEgressGroupId(Context context, String managementUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(EGRESS_GROUP_KEY_PREFIX + managementUrl, null);
    }

    public static void setSelectedEgressGroupId(Context context, String managementUrl, String groupId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (groupId == null) {
            prefs.edit().remove(EGRESS_GROUP_KEY_PREFIX + managementUrl).apply();
        } else {
            prefs.edit().putString(EGRESS_GROUP_KEY_PREFIX + managementUrl, groupId).apply();
        }
    }

    public static String getSelfPeerId(Context context, String managementUrl) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(SELF_PEER_ID_KEY_PREFIX + managementUrl, null);
    }

    public static void setSelfPeerId(Context context, String managementUrl, String peerId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(SELF_PEER_ID_KEY_PREFIX + managementUrl, peerId).apply();
    }
}
