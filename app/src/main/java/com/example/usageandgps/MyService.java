package com.example.usageandgps;

import static com.example.usageandgps.MainActivity.participationID;

import android.os.PowerManager;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
public class MyService extends Service {

    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean running = true;
    private static final String TAG = MyService.class.getSimpleName();
    private String locationString;
    private DatabaseReference mDatabase;
    private UsageStatsManager mUsageStatsManager;
    private final HashMap<String, String> map = new HashMap<>();
    String data;
    public static int tripCount = 0;
    public static List<String> tripTimes = new ArrayList<>();
    Location oldLocation;
    String provider;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String[] formattedTime = new String[1];
        data = participationID.getText().toString();
        Log.d(TAG, "Service started");
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);
        locationListener = new LocationListener() {
            private final Handler handler = new Handler(Looper.getMainLooper());
            private Runnable tripRunnable;
            private Location potentialTripLocation;

            @Override
            public void onLocationChanged(Location location) {
                LocalTime timeLocal = null;
                DateTimeFormatter formatter = null;
                if (location != null && location.getAccuracy() < 20) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    locationString = "Location: " + latitude + " / " + longitude;

                    if (oldLocation == null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            timeLocal = LocalTime.now();
                            formatter = DateTimeFormatter.ofPattern("HH:mm");
                            formattedTime[0] = timeLocal.format(formatter);
                        }
                        oldLocation = new Location(location);
                    }
                    if (oldLocation.distanceTo(location) > 5) {
                        if (potentialTripLocation == null) {
                            potentialTripLocation = new Location(location);
                            System.out.println("Trip started - Potential");
                            startTripTimer();
                        } else if (potentialTripLocation.distanceTo(location) > 15) {
                            potentialTripLocation.set(location);
                            System.out.println("Trip exceeded limit for 15 meters - Potential");
                            resetTripTimer();
                        }
                    } else {
                        System.out.println("Trip cancelled - 5 meters down");
                        resetTripTimer();
                    }
                    System.out.println(oldLocation.distanceTo(location));
                } else {
                    System.out.println("location is null");
                }
            }

            private void startTripTimer() {
                tripRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tripCount++;
                        System.out.println("Trip counted + 1:" + tripCount);
                        tripTimes.add(formattedTime[0]);
                        oldLocation = null;
                        potentialTripLocation = null;
                    }
                };
                handler.postDelayed(tripRunnable, 60 * 1000);
            }

            private void resetTripTimer() {
                if (tripRunnable != null) {
                    handler.removeCallbacks(tripRunnable);
                    potentialTripLocation = null;
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // no-op
            }

            @Override
            public void onProviderEnabled(String provider) {
                // no-op
            }

            @Override
            public void onProviderDisabled(String provider) {
                // no-op
            }
        };
        startTrackingAppUsage();
        return START_STICKY;
    }

    boolean isRunning = false;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.example.UBCCAAF";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(chan);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Usage and GPS tracking is running in background.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }


    private void startTrackingAppUsage() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, 1000, 1, locationListener);
        }
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                checkAppUsage();
            }
        };
        timer.schedule(timerTask, 0, 1000);
    }

    String currentApp = "";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint({"SimpleDateFormat", "MissingPermission"})
    private void checkAppUsage() {
        if (!running) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.SECOND, -1);
        long startTime = calendar.getTimeInMillis();
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(c.getTime());
        String time;
        Location lastKnownLocation;
        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentApp = event.getPackageName();
                isRunning = true;
            }
        }
        if(isRunning){
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                double latitude = lastKnownLocation.getLatitude();
                double longitude = lastKnownLocation.getLongitude();
                locationString = "Location: " + latitude + " / " + longitude;
            }
            LocalTime timeLocal = LocalTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedTime = timeLocal.format(formatter);
            time = "Time: " + formattedTime;
            String pushedInfo = time + ", App used: " + currentApp + ", " + locationString;
            if (!map.containsKey(time)) {
                mDatabase.child("app_usage").child(formattedDate).child(data).push().setValue(pushedInfo);
                map.put(time, "App Used: " + currentApp + ", " + locationString);
            }
        }

    }



    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            startForeground(1, new Notification());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        MainActivity.releaseWakeLock();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}



