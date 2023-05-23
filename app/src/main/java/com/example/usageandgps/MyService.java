// Main package of the Android app
package com.example.usageandgps;

// Import necessary libraries and packages
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

// Main service class for tracking app usage and GPS location
public class MyService extends Service {

    // Define global variables and objects
    private LocationManager locationManager; // manages all location providers
    private LocationListener locationListener; // listener for location updates
    private boolean running = true; // flag to keep track of the service state
    private static final String TAG = MyService.class.getSimpleName(); // tag for debugging
    private String locationString; // string to hold location data
    private DatabaseReference mDatabase; // reference to Firebase database
    private UsageStatsManager mUsageStatsManager; // manager to interact with app usage statistics
    private final HashMap<String, String> map = new HashMap<>(); // map to store usage data
    private String data; // string to hold participation code
    public static int tripCount = 0; // counter for the number of trips
    public static List<String> tripTimes = new ArrayList<>(); // list to store the times of each trip
    private Location oldLocation; // previous location
    private String provider; // provider for location updates
    private boolean isRunning = false; // variable that holds if app is running or not
    private String currentApp = "";
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String[] formattedTime = new String[1]; // array to hold the formatted time
        data = participationID.getText().toString(); // get participation ID
        Log.d(TAG, "Service started"); // log the start of the service
        mDatabase = FirebaseDatabase.getInstance().getReference(); // get reference to Firebase database
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE); // get system service to track app usage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground(); // start service in foreground for Android Oreo and above
        }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE); // get system service to track location

        // Set the criteria for selecting the best provider
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE); // request fine accuracy
        provider = locationManager.getBestProvider(criteria, true); // get the best provider
        locationListener = new LocationListener() { // create new location listener
            // Handler to run tasks on main thread
            private final Handler handler = new Handler(Looper.getMainLooper());
            // Runnable to execute after a delay
            private Runnable tripRunnable;
            // Potential trip location
            private Location potentialTripLocation;

            // Method which gets called whenever the device location changes
            @Override
            public void onLocationChanged(Location location) {
                LocalTime timeLocal = null;
                DateTimeFormatter formatter = null;
                // Check if location is not null and accuracy is below 20
                if (location != null && location.getAccuracy() < 20) {
                    double latitude = location.getLatitude(); // get the latitude
                    double longitude = location.getLongitude(); // get the longitude
                    locationString = "Location: " + latitude + " / " + longitude; // create the location string

                    // Check if oldLocation is null, which indicates the start of a new trip
                    if (oldLocation == null) {
                        // Format the current time
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            timeLocal = LocalTime.now();
                            formatter = DateTimeFormatter.ofPattern("HH:mm");
                            formattedTime[0] = timeLocal.format(formatter);
                        }
                        oldLocation = new Location(location); // save the current location as old location
                    }
                    // Check if the device has moved more than 5 meters
                    if (oldLocation.distanceTo(location) > 5) {
                        // Check if potential trip has started
                        if (potentialTripLocation == null) {
                            potentialTripLocation = new Location(location); // save the current location as potential trip location
                            System.out.println("Trip started - Potential");
                            startTripTimer(); // start the trip timer
                        } else if (potentialTripLocation.distanceTo(location) > 15) {
                            potentialTripLocation.set(location); // update the potential trip location
                            System.out.println("Trip exceeded limit for 15 meters - Potential");
                            resetTripTimer(); // reset the trip timer
                        }
                    } else {
                        System.out.println("Trip cancelled - 5 meters down");
                        resetTripTimer(); // cancel the trip
                    }
                    System.out.println(oldLocation.distanceTo(location));
                } else {
                    System.out.println("location is null");
                }
            }


            // Method to start a timer for a potential trip
            private void startTripTimer() {
                tripRunnable = new Runnable() {
                    @Override
                    public void run() {
                        tripCount++; // increment trip count
                        System.out.println("Trip counted + 1:" + tripCount);
                        tripTimes.add(formattedTime[0]); // add the start time of the trip
                        oldLocation = null; // reset the old location
                        potentialTripLocation = null; // reset the potential trip location
                    }
                };
                // start the timer to run the tripRunnable after 60 seconds
                handler.postDelayed(tripRunnable, 60 * 1000);
            }

            // Method to reset the trip timer
            private void resetTripTimer() {
                if (tripRunnable != null) {
                    handler.removeCallbacks(tripRunnable); // remove any pending callbacks
                    potentialTripLocation = null; // reset the potential trip location
                }
            }

            // Overridden methods from LocationListener that are not used in this service
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
        startTrackingAppUsage(); // start tracking app usage
        return START_STICKY; // return START_STICKY so the service is recreated if the system kills it
    }

    // Method to start the service in the foreground
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String NOTIFICATION_CHANNEL_ID = "com.example.UBCCAAF"; // ID for the notification channel
        String channelName = "My Background Service"; // Name for the notification channel
        // Create a new notification channel
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.BLUE); // Set the color for the notification light
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE); // Set the visibility on the lock screen
        NotificationManager manager = getSystemService(NotificationManager.class); // Get the NotificationManager
        manager.createNotificationChannel(chan); // Create the notification channel
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID); // Create a builder for the notification
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.logo) // Set the small icon for the notification
                .setContentTitle("Usage and GPS tracking is running in background.") // Set the title of the notification
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority of the notification
                .setCategory(Notification.CATEGORY_SERVICE) // Set the category of the notification
                .build();
        startForeground(2, notification); // Start the service in the foreground with the notification
    }

    // Method to start tracking the app usage
    private void startTrackingAppUsage() {
        // Check if the app has permission to access fine location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(provider, 1000, 1, locationListener); // Request location updates
        }
        Timer timer = new Timer(); // Create a new timer
        TimerTask timerTask = new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                checkAppUsage(); // Check the app usage
            }
        };
        timer.schedule(timerTask, 0, 1000); // Schedule the timer task to run every second
    }

    // Method to check the app usage
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint({"SimpleDateFormat", "MissingPermission"})
    private void checkAppUsage() {
        if (!running) { // If the service is not running, return immediately
            return;
        }
        Calendar calendar = Calendar.getInstance(); // Get a Calendar instance
        long endTime = calendar.getTimeInMillis(); // Get the current time
        calendar.add(Calendar.SECOND, -1); // Subtract 1 second from the current time
        long startTime = calendar.getTimeInMillis(); // Get the start time
        // Query the usage stats for the last second
        UsageEvents usageEvents = mUsageStatsManager.queryEvents(startTime, endTime);
        Calendar c = Calendar.getInstance(); // Get a Calendar instance
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy"); // Format for the date
        String formattedDate = df.format(c.getTime()); // Get the formatted date
        String time; // Variable for the time
        Location lastKnownLocation; // Variable for the last known location
        while (usageEvents.hasNextEvent()) { // While there are more events
            UsageEvents.Event event = new UsageEvents.Event(); // Create a new Event
            usageEvents.getNextEvent(event); // Get the next event
            if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) { // If the event is an app moving to the foreground
                currentApp = event.getPackageName(); // Get the package name of the app
                isRunning = true; // Set isRunning to true
            }
        }
        if(isRunning){ // If an app is running
            // Get the last known location
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) { // If the last known location is not null
                double latitude = lastKnownLocation.getLatitude(); // Get the latitude
                double longitude = lastKnownLocation.getLongitude(); // Get the longitude
                locationString = "Location: " + latitude + " / " + longitude; // Set the location string
            }
            // Get the current local time
            LocalTime timeLocal = LocalTime.now();
            // Format for the time
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedTime = timeLocal.format(formatter); // Get the formatted time
            time = "Time: " + formattedTime; // Set the time
            // Format the information to be pushed to the database
            String pushedInfo = time + ", App used: " + currentApp + ", " + locationString;
            if (!map.containsKey(time)) { // If the map doesn't already contain the time
                // Push the information to the database
                mDatabase.child("app_usage").child(formattedDate).child(data).push().setValue(pushedInfo);
                map.put(time, "App Used: " + currentApp + ", " + locationString); // Put the information in the map
            }
        }
    }

    // Method called when the service is created
    @Override
    public void onCreate() {
        super.onCreate();
        // If the Build.VERSION is greater or equals to Oreo, start foreground with a specific notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {
            // If not, start the service in the foreground with a new notification
            startForeground(1, new Notification());
        }
    }

    // Method called when the service is destroyed
    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false; // Set running to false
        MainActivity.releaseWakeLock(); // Release the wake lock
        stopForeground(true); // Stop the service running in the foreground
        stopSelf(); // Stop the service
    }

    // Method called when the service is bound to a component
    @Override
    public IBinder onBind(Intent intent) {
        return null; // The service is not bound to any component, so return null
    }

}



