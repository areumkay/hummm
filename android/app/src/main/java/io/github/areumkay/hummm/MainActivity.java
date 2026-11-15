package io.github.areumkay.hummm;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Hosts the web app from assets in a WebView. The WebView is never paused,
 * so audio keeps running when the screen turns off; PlaybackService keeps
 * the process alive while sound is playing.
 */
public class MainActivity extends Activity {

    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "HummmAndroid");
        setContentView(web);

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl("file:///android_asset/index.html");
        }

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.POST_NOTIFICATIONS }, 1);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        // Let the page close an open sheet first; otherwise go to the background
        // instead of finishing, so playback carries on.
        web.evaluateJavascript("window.hummmBack ? window.hummmBack() : false", result -> {
            if (!"true".equals(result)) moveTaskToBack(true);
        });
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PlaybackService.class));
        web.destroy();
        super.onDestroy();
    }

    /** Called from the page's JavaScript as window.HummmAndroid. */
    private class Bridge {
        @JavascriptInterface
        public void setPlaying(boolean playing, double endAtMillis) {
            runOnUiThread(() -> {
                Intent i = new Intent(MainActivity.this, PlaybackService.class);
                if (playing) {
                    i.putExtra(PlaybackService.EXTRA_END_AT, (long) endAtMillis);
                    startForegroundService(i);
                } else {
                    stopService(i);
                }
            });
        }
    }
}
