package com.blutoothoff;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class BluetoothService extends Service {

    private static final String CHANNEL_ID = "BluetoothTimerChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private Handler handler;
    private Runnable timerRunnable;
    private long endTime;
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("minutes")) {
            int minutes = intent.getIntExtra("minutes", 5);
            startTimer(minutes);
        }
        return START_STICKY;
    }

    private void startTimer(int minutes) {
        if (isRunning) {
            return;
        }

        isRunning = true;
        endTime = System.currentTimeMillis() + (minutes * 60 * 1000);

        startForeground(NOTIFICATION_ID, createNotification("Timer running..."));

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingTime = endTime - System.currentTimeMillis();

                if (remainingTime <= 0) {
                    turnOffBluetooth();
                    stopSelf();
                } else {
                    // Update notification
                    updateNotification(remainingTime);
                    
                    // Broadcast update to UI
                    Intent updateIntent = new Intent("TIMER_UPDATE");
                    updateIntent.putExtra("remainingTime", remainingTime);
                    sendBroadcast(updateIntent);
                    
                    // Schedule next update
                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.post(timerRunnable);
    }

    private void turnOffBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && 
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BluetoothService", "Bluetooth permission missing");
                return;
            }
            
            boolean success = bluetoothAdapter.disable();
            Log.d("BluetoothService", "Bluetooth disable result: " + success);
            
            // Broadcast completion
            Intent finishIntent = new Intent("TIMER_FINISHED");
            finishIntent.putExtra("success", success);
            sendBroadcast(finishIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Timer",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Bluetooth auto-off timer");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String content) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bluetooth Timer")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void updateNotification(long remainingMillis) {
        long minutes = remainingMillis / 60000;
        long seconds = (remainingMillis % 60000) / 1000;
        String content = String.format("Time remaining: %02d:%02d", minutes, seconds);
        
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(content));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (handler != null && timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}