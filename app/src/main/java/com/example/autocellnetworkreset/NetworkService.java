package com.example.autocellnetworkreset;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class NetworkService extends Service {

    private static final String TAG = "NetworkService";
    private static final String CHANNEL_ID = "NetworkServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private int duration = 5; // Default duration
    private boolean isResetting = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onServiceStateChanged(ServiceState serviceState) {
                super.onServiceStateChanged(serviceState);
                String stateString;
                switch (serviceState.getState()) {
                    case ServiceState.STATE_IN_SERVICE:
                        stateString = "In Service";
                        break;
                    case ServiceState.STATE_OUT_OF_SERVICE:
                        stateString = "Out of Service";
                        break;
                    case ServiceState.STATE_EMERGENCY_ONLY:
                        stateString = "Emergency Only";
                        break;
                    case ServiceState.STATE_POWER_OFF:
                        stateString = "Radio Off";
                        break;
                    default:
                        stateString = "Unknown";
                        break;
                }
                updateNotification("Monitoring | State: " + stateString);
                Log.d(TAG, "Service state changed: " + serviceState.getState() + " (" + stateString + ")");


                if (isResetting) {
                    Log.d(TAG, "Reset already in progress, ignoring state change.");
                    return;
                }

                boolean isAirplaneModeOn = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
                if (isAirplaneModeOn) {
                    Log.d(TAG, "Airplane mode is on, ignoring state change.");
                    return;
                }

                if (serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE || serviceState.getState() == ServiceState.STATE_EMERGENCY_ONLY) {
                    Log.d(TAG, "Out of service or emergency only, starting network reset.");
                    resetNetwork();
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            duration = intent.getIntExtra("duration", 5);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Cell Network Reset")
                .setContentText("Monitoring cellular network state.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        Log.d(TAG, "Service started, listening for service state changes.");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        Log.d(TAG, "Service destroyed, stopped listening for service state changes.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Network Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void resetNetwork() {
        isResetting = true;
        updateNotification("Signal Lost! Resetting network...");
        Log.d(TAG, "Enabling airplane mode.");

        try {
            Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
            updateNotification("Airplane Mode ON... Waiting " + duration + "s.");
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable airplane mode. Check permissions.", e);
            updateNotification("Error: Permission denied. Cannot reset network.");
            isResetting = false; // Reset the flag so it can try again later
            return; // Stop the reset process
        }


        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Log.d(TAG, "Disabling airplane mode.");
            try {
                updateNotification("Turning Airplane Mode OFF...");
                Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

                // We don't reset the notification to "Monitoring" here immediately,
                // because we want the user to see the "OFF" message.
                // The next onServiceStateChanged event will update it.
                isResetting = false;
                Log.d(TAG, "Network reset complete.");
            } catch (Exception e) {
                Log.e(TAG, "Failed to disable airplane mode. Check permissions.", e);
                updateNotification("Error: Permission denied. Cannot complete network reset.");
                isResetting = false; // Reset the flag
            }
        }, duration * 1000L);
    }

    private void updateNotification(String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Auto Cell Network Reset")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, notification);
    }
}
