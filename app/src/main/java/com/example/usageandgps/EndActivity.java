package com.example.usageandgps;

import static com.example.usageandgps.MainActivity.participationID;
import static com.example.usageandgps.MyService.tripCount;
import static com.example.usageandgps.MyService.tripTimes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class EndActivity extends AppCompatActivity {

    Button btn;
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;
    private Button submit;
    private Button cancel;
    private DatabaseReference mDatabase;
    private EditText first;
    String timeForPop = "";
    private TextView textView;
    private String data;
    private int i = 0;
    private Button btnEdit;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_end);
        btn = findViewById(R.id.endButton);
        btnEdit = findViewById(R.id.editTrip);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serviceIntent = new Intent(getApplicationContext(), MyService.class);
                stopService(serviceIntent);
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), TripActivity.class);
                startActivity(intent);
            }
        });
        data = participationID.getText().toString();
        mDatabase = FirebaseDatabase.getInstance().getReference();


        mDatabase.child("TimeForPop").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                timeForPop = snapshot.getValue(String.class);
                showPopNTimes(timeForPop, tripCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("Error");
            }
        });

    }
    public void showPopNTimes(String timeString, int n) {
        if (n > 0) {
            showPop(timeString, n);
        }
    }

    public void showPop(String timeString, int remainingPopups) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Calendar now = Calendar.getInstance();
                @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                String formattedDate = df.format(now.getTime());
                @SuppressLint("SimpleDateFormat") SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                String formattedTime = timeFormat.format(now.getTime());
                if (formattedTime.equals(timeString)) {
                    runOnUiThread(new Runnable() {
                        @SuppressLint({"MissingInflatedId", "SetTextI18n"})
                        @Override
                        public void run() {
                            // Show the popup window
                            System.out.println("running");
                            final View popupWindow = getLayoutInflater().inflate(R.layout.popup_window_layout, null);

                            submit = popupWindow.findViewById(R.id.submit_button);
                            cancel = popupWindow.findViewById(R.id.cancel_button);
                            textView = popupWindow.findViewById(R.id.questionFirst);
                            first = popupWindow.findViewById(R.id.answerQuestion);
                            textView.setText("What was the way of transportation at " + tripTimes.get(tripCount-remainingPopups) + "?");
                            dialogBuilder = new AlertDialog.Builder(EndActivity.this);
                            dialogBuilder.setView(popupWindow);
                            dialog = dialogBuilder.create();
                            dialog.show();

                            submit.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String pushedInfo = first.getText().toString();
                                    mDatabase.child("app_usage").child(formattedDate).child(data).child("Prompt").push().setValue(pushedInfo);
                                    Toast.makeText(getApplicationContext(), "Your response is saved. ", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    showPopNTimes(timeString, remainingPopups - 1);
                                }
                            });

                            cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Toast.makeText(getApplicationContext(), "Your response is not saved. You cancelled it.", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                    showPopNTimes(timeString, remainingPopups - 1);
                                }
                            });
                        }
                    });
                }
            }
        }, 0, 1000 * 60); // Check every minute
    }

}
