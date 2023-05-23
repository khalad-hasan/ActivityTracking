package com.example.usageandgps;

import static com.example.usageandgps.MyService.tripTimes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TripActivity extends AppCompatActivity {
    private ListView tripListView;
    private TripListAdapter tripListAdapter;
    private Button backToMainMenuButton;
    private Button mergeSelectedTripsButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        tripListView = findViewById(R.id.trip_list_view);
        tripListAdapter = new TripListAdapter(this);
        tripListView.setAdapter(tripListAdapter);

        Button backToMainMenuButton = findViewById(R.id.back_to_main_menu_button);
        backToMainMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToEnd = new Intent(getApplicationContext(), EndActivity.class);
                startActivity(goToEnd);
            }
        });

        Button mergeSelectedTripsButton = findViewById(R.id.merge_selected_trips_button);
        mergeSelectedTripsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> selectedTripStartTimes = tripListAdapter.getSelectedTripStartTimes();
                System.out.println(selectedTripStartTimes);
                mergeSelectedTrips(selectedTripStartTimes);
            }
        });
    }
    private void mergeSelectedTrips(List<String> selectedTripStartTimes) {
        // Sort the selected trip start times
        Collections.sort(selectedTripStartTimes);

        System.out.println(selectedTripStartTimes);
        // Check if the selected trips are consecutive
        boolean areConsecutive = true;
        for (int i = 0; i < selectedTripStartTimes.size() - 1; i++) {
            int currentIndex = tripTimes.indexOf(selectedTripStartTimes.get(i));
            int nextIndex = tripTimes.indexOf(selectedTripStartTimes.get(i + 1));

            if (nextIndex - currentIndex != 1) {
                areConsecutive = false;
                break;
            }
        }

        if (!areConsecutive) {
            // Show a message to the user that only consecutive trips can be merged
            Toast.makeText(TripActivity.this, "Only consecutive trips can be merged", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTripStartTimes.size() >= 2) {
            // Merging logic: take the earliest start time as the new start time for the merged trip
            String earliestStartTime = selectedTripStartTimes.get(0);

            // Remove the original selected trips from the list
            tripTimes.removeAll(selectedTripStartTimes);

            // Add the merged trip to the list
            tripTimes.add(earliestStartTime);

            // Update the list view
            tripListAdapter.notifyDataSetChanged();

            Toast.makeText(TripActivity.this, "Trips are successfully merged.", Toast.LENGTH_SHORT).show();
            Collections.sort(tripTimes);
        }else{
            Toast.makeText(TripActivity.this, "At least 2 trips are required to merge.", Toast.LENGTH_SHORT).show();
        }
}

}
