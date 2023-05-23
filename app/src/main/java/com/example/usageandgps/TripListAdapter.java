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
    private final Context context;
    private final List<String> tripStartTimes;
    private SparseBooleanArray checkedStates;

    public TripListAdapter(Context context) {
        super(context, 0, MyService.tripTimes);
        this.context = context;
        this.tripStartTimes = MyService.tripTimes;
        checkedStates = new SparseBooleanArray();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_trip, parent, false);
        }

        String currentTripStartTime = tripStartTimes.get(position);

        CheckBox tripCheckbox = convertView.findViewById(R.id.trip_checkbox);
        TextView tripStartTime = convertView.findViewById(R.id.trip_start_time);

        tripStartTime.setText(currentTripStartTime);


        tripCheckbox.setOnCheckedChangeListener(null);
        tripCheckbox.setChecked(checkedStates.get(position, false));
        tripCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkedStates.put(position, isChecked);
            }
        });

        return convertView;
    }

    public List<String> getSelectedTripStartTimes() {
        List<String> selectedTripStartTimes = new ArrayList<>();
        for (int i = 0; i < tripStartTimes.size(); i++) {
            if (checkedStates.get(i, false)) {
                selectedTripStartTimes.add(tripStartTimes.get(i));
            }
        }
        System.out.println(selectedTripStartTimes);
        return selectedTripStartTimes;
    }
}
