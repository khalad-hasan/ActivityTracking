package com.example.usageandgps;

import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;


import androidx.annotation.RequiresApi;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.List;

public class AppUsageService extends Service {
    private UsageStatsManager mUsageStatsManager;
    private DatabaseReference mDatabase;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void onCreate() {
        super.onCreate();
        mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.d("ServiceStart", "Service started");
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    SystemClock.sleep(30000);
                    Calendar calendar = Calendar.getInstance();
                    long endTime = calendar.getTimeInMillis();
                    calendar.add(Calendar.HOUR_OF_DAY, -2);
                    long startTime = calendar.getTimeInMillis();

                    List<UsageStats> usageStatsList = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

                    for (UsageStats usageStats : usageStatsList) {
                        String packageName = usageStats.getPackageName();
                        long timeInForeground = usageStats.getTotalTimeInForeground();
                        Log.d("AppUsageService", packageName + ": " + timeInForeground);
                        if (timeInForeground != 0)
                            mDatabase.child("app_usage").child(packageName.replaceAll("\\.", " ")).setValue(timeInForeground/1000+" seconds");
                    }
                }
            }
        }).start();

        Log.d("ServiceEnd", "Service Ended");
        return START_STICKY;
    }
}
