package com.example.usageandgps;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TripListAdapter extends ArrayAdapter<String> {
    private final Context context;  // Application context
    private final List<String> tripStartTimes;  // List to hold trip start times
    private SparseBooleanArray checkedStates;  // To keep track of checked states of list items

    // Constructor initializing the context and tripStartTimes fields
    public TripListAdapter(Context context) {
        super(context, 0, MyService.tripTimes);
        this.context = context;
        this.tripStartTimes = MyService.tripTimes;
        checkedStates = new SparseBooleanArray();
    }

    // Method to inflate and customize the view for each list item
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_trip, parent, false);
        }

        // Get current trip start time
        String currentTripStartTime = tripStartTimes.get(position);

        // Find checkbox and textview for this list item
        CheckBox tripCheckbox = convertView.findViewById(R.id.trip_checkbox);
        TextView tripStartTime = convertView.findViewById(R.id.trip_start_time);

        // Set the text for trip start time
        tripStartTime.setText(currentTripStartTime);

        // Set checked state of checkbox based on SparseBooleanArray
        tripCheckbox.setOnCheckedChangeListener(null);
        tripCheckbox.setChecked(checkedStates.get(position, false));
        tripCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Update the checked state of current item position
                checkedStates.put(position, isChecked);
            }
        });

        return convertView;
    }

    // Method to get all selected trip start times
    public List<String> getSelectedTripStartTimes() {
        List<String> selectedTripStartTimes = new ArrayList<>();
        for (int i = 0; i < tripStartTimes.size(); i++) {
            // If current item is checked, add its start time to selectedTripStartTimes
            if (checkedStates.get(i, false)) {
                selectedTripStartTimes.add(tripStartTimes.get(i));
            }
        }
        System.out.println(selectedTripStartTimes);
        return selectedTripStartTimes;
    }
}

