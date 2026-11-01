package com.officerkd6.player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.Base64;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

/** Bridges the web app's media controls to a native notification-panel player. */
@CapacitorPlugin(name = "MediaSession")
public class MediaSessionPlugin extends Plugin {

    private static final Map<String, PluginCall> handlers = new ConcurrentHashMap<>();

    /** Called by the service when a button is pressed in the notification panel. */
    static void emit(String action, JSObject data) {
        PluginCall c = handlers.get(action);
        if (c == null) return;
        try {
            c.resolve(data == null ? new JSObject() : data);
        } catch (Throwable ignored) {
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void setActionHandler(PluginCall call) {
        String action = call.getString("action");
        if (action == null) {
            call.reject("action required");
            return;
        }
        call.setKeepAlive(true);
        try {
            getBridge().saveCall(call);
        } catch (Throwable ignored) {
        }
        handlers.put(action, call);
    }

    @PluginMethod
    public void setMetadata(PluginCall call) {
        MediaPlaybackService.title = call.getString("title", "");
        MediaPlaybackService.artist = call.getString("artist", "");
        MediaPlaybackService.album = call.getString("album", "");
        Bitmap bmp = null;
        try {
            JSArray arr = call.getArray("artwork");
            if (arr != null && arr.length() > 0) {
                JSONObject o = arr.getJSONObject(0);
                String src = o.optString("src", "");
                int comma = src.indexOf(',');
                if (src.startsWith("data:") && comma > 0) {
                    byte[] b = Base64.decode(src.substring(comma + 1), Base64.DEFAULT);
                    bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                }
            }
        } catch (Throwable ignored) {
        }
        MediaPlaybackService.art = bmp;
        MediaPlaybackService.sync(getContext());
        call.resolve();
    }

    @PluginMethod
    public void setPlaybackState(PluginCall call) {
        String st = call.getString("playbackState", "none");
        boolean play = "playing".equals(st);
        if (play) MediaPlaybackService.stopped = false;
        MediaPlaybackService.playing = play;
        MediaPlaybackService.active = !"none".equals(st) && !MediaPlaybackService.stopped;
        MediaPlaybackService.sync(getContext());
        call.resolve();
    }

    @PluginMethod
    public void setPositionState(PluginCall call) {
        Double dur = call.getDouble("duration", 0.0);
        Double pos = call.getDouble("position", 0.0);
        Double rate = call.getDouble("playbackRate", 1.0);
        MediaPlaybackService.durMs = (long) (dur * 1000);
        MediaPlaybackService.posMs = (long) (pos * 1000);
        MediaPlaybackService.rate = rate.floatValue();
        MediaPlaybackService.posStamp = SystemClock.elapsedRealtime();
        MediaPlaybackService.syncSession();
        call.resolve();
    }
}
