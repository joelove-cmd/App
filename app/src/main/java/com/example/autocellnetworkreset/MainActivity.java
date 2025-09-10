package com.example.autocellnetworkreset;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AutoCellNetworkResetPrefs";
    private static final String SERVICE_ENABLED_KEY = "serviceEnabled";
    private static final String DURATION_KEY = "duration";
    private static final int PERMISSION_REQUEST_CODE = 1;

    private SwitchMaterial serviceToggleSwitch;
    private EditText durationEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceToggleSwitch = findViewById(R.id.service_toggle_switch);
        durationEditText = findViewById(R.id.duration_edit_text);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isServiceEnabled = prefs.getBoolean(SERVICE_ENABLED_KEY, false);
        int duration = prefs.getInt(DURATION_KEY, 5);

        serviceToggleSwitch.setChecked(isServiceEnabled);
        durationEditText.setText(String.valueOf(duration));

        serviceToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (checkPermissions()) {
                    startNetworkService();
                } else {
                    requestPermissions();
                }
            } else {
                stopNetworkService();
            }
            savePreferences();
        });

        durationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                savePreferences();
                // If service is running, restart it with the new duration
                if (serviceToggleSwitch.isChecked()) {
                    stopNetworkService();
                    startNetworkService();
                }
            }
        });
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNetworkService();
            } else {
                Toast.makeText(this, "Permission denied. Service cannot start.", Toast.LENGTH_SHORT).show();
                serviceToggleSwitch.setChecked(false);
            }
        }
    }

    private void startNetworkService() {
        Intent serviceIntent = new Intent(this, NetworkService.class);
        int duration = 5;
        try {
            duration = Integer.parseInt(durationEditText.getText().toString());
        } catch (NumberFormatException e) {
            // Keep default duration
        }
        serviceIntent.putExtra("duration", duration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopNetworkService() {
        Intent serviceIntent = new Intent(this, NetworkService.class);
        stopService(serviceIntent);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(SERVICE_ENABLED_KEY, serviceToggleSwitch.isChecked());
        try {
            editor.putInt(DURATION_KEY, Integer.parseInt(durationEditText.getText().toString()));
        } catch (NumberFormatException e) {
            // Don't save if the format is invalid
        }
        editor.apply();
    }
}
