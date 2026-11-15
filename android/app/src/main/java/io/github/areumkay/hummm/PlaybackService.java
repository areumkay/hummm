package io.github.areumkay.hummm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Foreground service that runs while beats are playing. It holds a partial
 * wake lock so the CPU keeps feeding audio with the screen off. The lock
 * times out shortly after the sleep timer ends, as a safety net.
 */
public class PlaybackService extends Service {

    static final String EXTRA_END_AT = "endAt";
    private static final String CHANNEL = "playback";
    private static final long NO_TIMER_LIMIT_MS = 12L * 60 * 60 * 1000;
    private static final long TIMER_GRACE_MS = 60_000;

    private PowerManager.WakeLock lock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long endAt = intent != null ? intent.getLongExtra(EXTRA_END_AT, 0) : 0;

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
                new NotificationChannel(CHANNEL, "Playback", NotificationManager.IMPORTANCE_LOW));

        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder nb = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Hummm")
                .setContentText(endAt > 0 ? "Playing · sleep timer on" : "Playing")
                .setContentIntent(tap)
                .setOngoing(true);
        if (endAt > 0) {
            nb.setWhen(endAt).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true);
        }

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, nb.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, nb.build());
        }

        if (lock == null) {
            PowerManager pm = getSystemService(PowerManager.class);
            lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hummm:playback");
            lock.setReferenceCounted(false);
        }
        long timeout = endAt > 0
                ? Math.max(TIMER_GRACE_MS, endAt - System.currentTimeMillis() + TIMER_GRACE_MS)
                : NO_TIMER_LIMIT_MS;
        lock.acquire(timeout);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (lock != null && lock.isHeld()) lock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
