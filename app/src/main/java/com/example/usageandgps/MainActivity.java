// Main package of the Android app
package com.example.usageandgps;

// Import necessary libraries and packages
import androidx.annotation.NonNull;
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
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    // Static constants for service class name and location request
    private static final String SERVICE_CLASS_NAME = "com.example.usageandgps.MyService";
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    // Database reference for Firebase
    private DatabaseReference mDatabase;

    // Edit text to accept participation ID from user
    @SuppressLint("StaticFieldLeak")
    public static EditText participationID;

    // List to store participation codes fetched from Firebase
    private ArrayList<Integer> codes = new ArrayList<>();

    // Static constants for usage stats and location permissions
    private static final int MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS = 100;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 123;

    // onCreate method where all initial setup is done
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Acquire wake lock to keep device awake
        acquireWakeLock();

        // Initialize Firebase database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Check if our service is already running
        boolean isServiceRunning = isServiceRunning(this, SERVICE_CLASS_NAME);

        // If the service is running, start EndActivity
        if (isServiceRunning) {
            Intent intent1 = new Intent(this, EndActivity.class);
            startActivity(intent1);
        }

        // Get the EditText from layout to input participation ID
        participationID = findViewById(R.id.ID);

        // Fetch participation codes from Firebase
        mDatabase.child("ParticipationCodes").get().addOnSuccessListener(new OnSuccessListener<DataSnapshot>() {
            @Override
            public void onSuccess(DataSnapshot dataSnapshot) {
                for(DataSnapshot child: dataSnapshot.getChildren()){
                    codes.add(child.getValue(Integer.class));
                }
            }
        });

    }

    // When the button is pressed
    public void buttonPressed(View v) {
        // Check if the entered participation code is valid
        if(codes.contains(Integer.parseInt(participationID.getText().toString()))){
            // If valid, request usage stats permission
            requestUsageStatsPermission();
        }else{
            // If not valid, show an error message
            Toast.makeText(this, "Participation Code is not correct. Please try again." , Toast.LENGTH_SHORT).show();
        }
    }

    // Method to request usage stats permission
    private void requestUsageStatsPermission() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            // If permission not granted, request it
            startActivityForResult(intent, MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS);

            // Also check and request for location permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted, request it
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
        } else {
            // If permission is already granted
            Toast.makeText(this, "Access is already granted." , Toast.LENGTH_SHORT).show();
            onActivityResult(MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS, MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS,intent);
        }
    }

    // Method called when permissions are granted or denied
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS) {
            // If usage stats permission is granted
            if (hasUsageStatsPermission()) {
                // Start our service with the participation ID
                Intent intent = new Intent(this, MyService.class);
                intent.putExtra("participationID", participationID.getText().toString());
                startService(intent);

                // Start EndActivity
                Intent intent1 = new Intent(this, EndActivity.class);
                startActivity(intent1);
                Toast.makeText(this,"Permission granted. You are good to go.", Toast.LENGTH_LONG).show();
            } else {
                // If permission is denied
                Toast.makeText(this,"Permission denied. Please try again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Check if we have usage stats permission
    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager)
                getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // Check if a service is running
    private boolean isServiceRunning(Context context, String serviceClassName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true;
            }
        }
        return false;
    }

    // Acquire a wake lock to keep device awake
    private static PowerManager.WakeLock wakeLock;
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyService::LocationTrackingWakeLock");
        wakeLock.acquire();
    }

    // Release the wake lock
    static void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
