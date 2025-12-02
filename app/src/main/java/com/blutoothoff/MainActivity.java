package com.blutoothoff;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextView bluetoothStatus;
    private TextView timerDisplay;
    private TextInputEditText timerInput;
    private MaterialButton startButton;
    private MaterialButton cancelButton;
    private MaterialCardView timerInputCard;
    private MaterialCardView timerDisplayCard;
    private View statusIndicator;
    
    // Preset buttons
    private MaterialButton preset5;
    private MaterialButton preset15;
    private MaterialButton preset30;
    private MaterialButton preset60;
    
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private Runnable updateTimerRunnable;

    private BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                updateBluetoothStatus();
            }
        }
    };

    private BroadcastReceiver timerUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("TIMER_UPDATE".equals(intent.getAction())) {
                long remainingTime = intent.getLongExtra("remainingTime", 0);
                updateTimerDisplay(remainingTime);
            } else if ("TIMER_FINISHED".equals(intent.getAction())) {
                resetUI();
                boolean success = intent.getBooleanExtra("success", true);
                if (success) {
                    Toast.makeText(MainActivity.this, "Bluetooth turned off!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to turn off Bluetooth automatically", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initBluetooth();
        setupListeners();
        checkPermissions();
        
        handler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        bluetoothStatus = findViewById(R.id.bluetoothStatus);
        timerDisplay = findViewById(R.id.timerDisplay);
        timerInput = findViewById(R.id.timerInput);
        startButton = findViewById(R.id.startButton);
        cancelButton = findViewById(R.id.cancelButton);
        timerInputCard = findViewById(R.id.timerInputCard);
        timerDisplayCard = findViewById(R.id.timerDisplayCard);
        statusIndicator = findViewById(R.id.statusIndicator);
        
        // Preset buttons
        preset5 = findViewById(R.id.preset5);
        preset15 = findViewById(R.id.preset15);
        preset30 = findViewById(R.id.preset30);
        preset60 = findViewById(R.id.preset60);
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        updateBluetoothStatus();
    }

    private void setupListeners() {
        startButton.setOnClickListener(v -> startTimer());
        cancelButton.setOnClickListener(v -> cancelTimer());
        
        // Preset button listeners
        preset5.setOnClickListener(v -> setPresetTime(5));
        preset15.setOnClickListener(v -> setPresetTime(15));
        preset30.setOnClickListener(v -> setPresetTime(30));
        preset60.setOnClickListener(v -> setPresetTime(60));
    }
    
    private void setPresetTime(int minutes) {
        timerInput.setText(String.valueOf(minutes));
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            String[] permissions = {
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.SCHEDULE_EXACT_ALARM
            };

            boolean allGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Permissions required for app to work", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateBluetoothStatus() {
        if (bluetoothAdapter != null) {
            boolean isEnabled = bluetoothAdapter.isEnabled();
            bluetoothStatus.setText(isEnabled ? "ON" : "OFF");
            bluetoothStatus.setTextColor(getColor(
                isEnabled ? R.color.bluetooth_on : R.color.bluetooth_off));
            
            // Update status indicator
            statusIndicator.setBackgroundResource(
                isEnabled ? R.drawable.status_indicator_on : R.drawable.status_indicator_off);
        }
    }

    private void startTimer() {
        String input = timerInput.getText() != null ? timerInput.getText().toString() : "";
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter time in minutes", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is already OFF", Toast.LENGTH_SHORT).show();
            return;
        }

        int minutes = Integer.parseInt(input);
        if (minutes <= 0 || minutes > 999) {
            Toast.makeText(this, "Please enter time between 1-999 minutes", 
                Toast.LENGTH_SHORT).show();
            return;
        }

        // Start service
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        serviceIntent.putExtra("minutes", minutes);
        startForegroundService(serviceIntent);

        // Update UI - show timer display, hide input card
        timerInputCard.setVisibility(View.GONE);
        timerDisplayCard.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.VISIBLE);

        Toast.makeText(this, "Timer started for " + minutes + " minutes", 
            Toast.LENGTH_SHORT).show();
    }

    private void cancelTimer() {
        Intent serviceIntent = new Intent(this, BluetoothService.class);
        stopService(serviceIntent);
        resetUI();
        Toast.makeText(this, "Timer cancelled", Toast.LENGTH_SHORT).show();
    }

    private void resetUI() {
        timerInputCard.setVisibility(View.VISIBLE);
        timerDisplayCard.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.GONE);
        timerInput.setText("");
        timerDisplay.setText("00:00");
    }

    private void updateTimerDisplay(long remainingMillis) {
        long minutes = remainingMillis / 60000;
        long seconds = (remainingMillis % 60000) / 1000;
        String timeText = String.format("%02d:%02d", minutes, seconds);
        timerDisplay.setText(timeText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter stateFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, stateFilter, Context.RECEIVER_NOT_EXPORTED);
        
        IntentFilter timerFilter = new IntentFilter();
        timerFilter.addAction("TIMER_UPDATE");
        timerFilter.addAction("TIMER_FINISHED");
        registerReceiver(timerUpdateReceiver, timerFilter, Context.RECEIVER_NOT_EXPORTED);
        
        updateBluetoothStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(timerUpdateReceiver);
    }
}