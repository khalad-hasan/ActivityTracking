package com.example.usageandgps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String SERVICE_CLASS_NAME = "com.example.usageandgps.MyService";
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    @SuppressLint("StaticFieldLeak")
    public static EditText participationID;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean isServiceRunning = isServiceRunning(this, SERVICE_CLASS_NAME);
        if (isServiceRunning) {
            Intent intent1 = new Intent(this, EndActivity.class);
            startActivity(intent1);
        }

        participationID = findViewById(R.id.ID);
    }
    public void buttonPressed(View v) {
        requestUsageStatsPermission();
    }

    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 100;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        } else {
            Toast.makeText(this, "Access is already granted." , Toast.LENGTH_SHORT).show();
            onActivityResult(MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS, MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS,intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            if (hasUsageStatsPermission()) {
                Intent intent = new Intent(this, MyService.class);
                intent.putExtra("participationID", participationID.getText().toString());
                startService(intent);
                Intent intent1 = new Intent(this, EndActivity.class);
                startActivity(intent1);
                Toast.makeText(this,"Permission granted. You are good to go.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,"Permission denied. Please try again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isServiceRunning(Context context, String serviceClassName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }
        return false;
    }
}