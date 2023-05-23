# Android Application: Trip Tracking - UBC CAAF

This repository contains the source code for a trip tracking Android application. The application collects and displays data about trips, which can be edited and managed by the user.

The app is structured around five primary classes: 

1. `MainActivity`: This is the entry point of the application. It sets up the initial UI and initiates the tracking service.
2. `MyService`: This class is the whole running logic of the application. 
3. `EndActivity`: This class handles the termination of the trip tracking service and manages data persistence.
4. `TripActivity`: This class provides a UI for managing individual trips tracked by the application.
5. `TripListAdapter`: This class extends `ArrayAdapter<String>`. It's responsible for the customization of the ListView items in the `TripActivity` class.

The following sections provide detailed documentation for each of these classes, including their constructors, methods, and key interactions.

Please refer to the individual class documentations below for detailed understanding of their respective functionalities.

# MainActivity - Android Application Class Documentation

`MainActivity` class is the entry point of the Android application. The class extends `AppCompatActivity` to make use of the compatibility features.

This class performs several key functions:

- Collects participation IDs.
- Requests user permissions for accessing usage stats and location.
- Launches the main service (`MyService`) of the application.
- Manages wake locks to ensure the device stays awake for the operation.

## Class Fields

- `SERVICE_CLASS_NAME`: A constant holding the name of the service class.
- `REQUEST_LOCATION_PERMISSION`: Constant integer for location permission request code.
- `mDatabase`: A `DatabaseReference` instance to interact with Firebase database.
- `participationID`: A static `EditText` field to get the participation ID input from the user.
- `check`: A boolean variable, though it is declared, it isn't used anywhere in the class.
- `codes`: An `ArrayList<Integer>` field to store participation codes fetched from the Firebase database.
- `wakeLock`: A `PowerManager.WakeLock` object to acquire and release wake locks.
- `MY_PERMISSIONS_REQUEST_PACKAGE_USAGE_STATS` and `MY_PERMISSIONS_REQUEST_LOCATION`: Request codes for usage stats and location permissions.

## Key Methods

### `onCreate(Bundle savedInstanceState)`

The method runs when the activity is first created. It initializes the Firebase database reference, acquires the wake lock, checks if the service is running, and fetches participation codes from Firebase.

### `buttonPressed(View v)`

This method is called when the button is pressed. It checks whether the entered participation code exists in the fetched list of codes. If it exists, the method proceeds to request usage stats permission.

### `requestUsageStatsPermission()`

This method initiates the request for usage stats permissions. It also checks and requests location permissions if they are not granted already.

### `onActivityResult(int requestCode, int resultCode, Intent data)`

This method is called when permissions are granted or denied. If the usage stats permission is granted, it starts the service with the participation ID and also launches the `EndActivity`.

### `hasUsageStatsPermission()`

This helper method checks whether usage stats permission is granted.

### `isServiceRunning(Context context, String serviceClassName)`

This helper method checks whether the given service is running.

### `acquireWakeLock()`

This method acquires a wake lock to keep the device awake.

### `releaseWakeLock()`

This static method releases the wake lock if it is held.

## Note

Ensure the permissions and wake locks are handled properly, respecting user's privacy and device battery life.

# MyService Class

The `MyService` class is an Android `Service` class used in the application to track user activity and GPS location. This service is designed to run continuously in the foreground, providing consistent access to location data and tracking the applications used by the device user. The data collected is sent to a Firebase Realtime Database.

## Key Class Features

### Location Tracking
The service uses the `LocationManager` and `LocationListener` Android components to track user location with fine accuracy. The `LocationListener` implements four methods: `onLocationChanged`, `onStatusChanged`, `onProviderEnabled`, and `onProviderDisabled`. The `onLocationChanged` method is triggered when the user's location changes. Here, it manages the logic of defining and tracking a trip. A trip starts when the user has moved more than 5 meters from the initial location and lasts for at least 60 seconds.

### Application Usage Tracking
The service also uses the `UsageStatsManager` system service to track the apps used on the device. This functionality is implemented in the `checkAppUsage` method. It retrieves the application used in the last second and the current location, then pushes this data to a Firebase Realtime Database. This method requires explicit permission from the user to access app usage statistics.

### Foreground Service
`MyService` is implemented as a foreground service to ensure that it keeps running, even if the app is not in use. The `startMyOwnForeground` method creates a notification channel and sets a persistent notification for the foreground service. It's called within the `onStartCommand` and `onCreate` methods to ensure that the service remains in the foreground state.

## Main Methods

### onStartCommand(Intent intent, int flags, int startId)
This method initializes key components, such as the Firebase database reference and the UsageStatsManager. It also determines the best location provider based on the accuracy criterion and sets up the location listener. Once the location listener is set up, it starts tracking app usage.

### startMyOwnForeground()
This method creates a notification channel for Android Oreo and later versions. It then sets up a notification informing the user that the service is running. This notification allows the service to run in the foreground, thereby preventing it from being killed by the system due to low resources.

### startTrackingAppUsage()
This method checks if the application has the necessary location permission. If the permission is granted, it requests location updates and begins to track application usage by scheduling a repeating task that calls `checkAppUsage` every second.

### checkAppUsage()
This method checks the app usage for the past second, retrieves the most recently used app, and, if it's running, fetches the last known location. It formats the current time, app used, and location as a string and sends it to Firebase, ensuring that the same data is not sent twice.

### onCreate()
This method is called when the service is created. It checks the Android version and, if necessary, starts the service in the foreground.

### onDestroy()
This method is called when the service is destroyed. It stops the service running in the foreground, stops the service itself, and releases the wake lock.

## Usage and Integration
The `MyService` class is intended to be used as a part of an Android application that requires user location and app usage tracking. The data collected can be used for a variety of purposes, such as studying user behavior, providing location-based features, or monitoring app usage.

## Permissions
For the service to function correctly, the application must have the following permissions:

- ACCESS_FINE_LOCATION: To access the user's precise location.
- PACKAGE_USAGE_STATS: To access application


# EndActivity Class

This class is part of the Android Activity lifecycle. This specific `EndActivity` is used to stop the ongoing service and edit the trip information in the app.

## Fields

- `btn`: A Button object used to stop the ongoing service.
- `btnEdit`: A Button object used to go to `TripActivity` for editing trip information.
- `dialogBuilder`: An AlertDialog.Builder object used to build a dialog box for displaying popup.
- `dialog`: An AlertDialog object which represents the popup window.
- `mDatabase`: A DatabaseReference object used for interactions with Firebase Database.
- `first`: An EditText object for user to input the answer to the question in the popup.
- `textView`: A TextView object to display questions in the popup.
- `timeForPop`: A string object to store the time at which the popup should be displayed.
- `data`: A string object to store the participationID.

## Methods

- `onCreate()`: This method initializes the activity. It sets up the layout and the views (like buttons), and sets up click listeners for the buttons. It also initializes the Firebase database reference and fetches the `TimeForPop` data from the database.
- `showPopNTimes(String timeString, int n)`: This method is used to display a popup 'n' number of times at a specified time. It calls the `showPop()` method if 'n' is greater than 0.
- `showPop(String timeString, int remainingPopups)`: This method sets a timer to display a popup at a specified time. It repeats this task every minute.

# TripActivity Class

This class is an Android activity which handles trip-related operations, like viewing and merging trips.

## Fields

- `tripListView`: A ListView object used to display the list of trips.
- `tripListAdapter`: A custom adapter (TripListAdapter) for displaying the trip items in the ListView.
- `backToMainMenuButton`: A Button object which, when clicked, takes the user back to the main menu.
- `mergeSelectedTripsButton`: A Button object which, when clicked, merges selected trips if they meet the merging conditions.

## Methods

- `onCreate()`: This is the initialization callback of the activity. It sets up the layout and the views (like buttons and ListView), sets up the ListView adapter, and sets click listeners for the buttons.
- `mergeSelectedTrips(List<String> selectedTripStartTimes)`: This method merges the selected trips if they are consecutive and there are at least two of them. The start time of the merged trip is the earliest start time among the selected trips. If the selected trips are not consecutive, it shows a toast message to inform the user. After merging, it removes the original selected trips from the list and adds the merged trip, then updates the ListView. It also shows a toast message to indicate whether the merging was successful.

# Note

## Participation Codes are from 0 to 19 included. You can change them in firebase accordingly. 
## Firebase link: https://console.firebase.google.com/u/0/project/usageandgps/database/usageandgps-default-rtdb/data 

For an access to Firebase, please contact me at alakbarzadenihad@gmail.com.

