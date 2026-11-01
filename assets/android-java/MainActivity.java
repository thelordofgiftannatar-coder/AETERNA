package com.officerkd6.player;

import android.os.Build;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MediaSessionPlugin.class);
        super.onCreate(savedInstanceState);
        // Android 13+: notifications (and so the media panel) need permission
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1001);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // keep the WebView (and its audio) alive while the screen is off / app in background
        try {
            getBridge().getWebView().onResume();
            getBridge().getWebView().resumeTimers();
        } catch (Throwable ignored) {
        }
    }
}
