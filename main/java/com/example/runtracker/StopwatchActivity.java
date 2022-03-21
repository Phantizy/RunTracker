package com.example.runtracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Timer;
import java.util.TimerTask;

public class StopwatchActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView hoursTextView;
    private TextView minsTextView;
    private TextView secsTextView;
    private TextView tenthsTextView;

    private Button resetButton;
    private Button startStopButton;


    private long startTimeMillis;
    private long elapsedTimeMillis;

    private int elapsedHours;
    private int elapsedMins;
    private int elapsedSecs;
    private int elapsedTenths;

    private Timer timer;
    private NumberFormat number;

    private SharedPreferences prefs;
    private boolean stopwatchOn;

    private Intent servicesIntent;

    private final int NOTIFICATION_ID =1;
    private NotificationManager notificationManager;
    //private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stopwatch);

        // get the references to widgets
        hoursTextView = findViewById(R.id.textViewHoursValue);
        minsTextView = findViewById(R.id.textviewMinsValue);
        secsTextView = findViewById(R.id.textviewSecsValue);
        tenthsTextView = findViewById(R.id.textViewTenthsValue);
        resetButton = findViewById(R.id.buttonReset);
        startStopButton = findViewById(R.id.buttonStartStop);


        // set listeners
        resetButton.setOnClickListener(this);
        startStopButton.setOnClickListener(this);


        // get preferences
        prefs = getSharedPreferences("Prefs", MODE_PRIVATE);

        // create intents
        servicesIntent = new Intent(this, RunTrackerService.class);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonStartStop:
                if (stopwatchOn){
                    stop();
                }else{
                    start();
                }
                break;
            case R.id.buttonReset:
                reset();
                break;
        }
    }

    private void reset() {
        // stop timer
        this.stop();

        // reset millis and update views
        elapsedTimeMillis = 0;
        updateViews(elapsedTimeMillis);

    }

    private void start() {
        // make sure old timer thread has been cancelled
        if(timer != null){
            timer.cancel();
        }

        // if stopped or reset, set new start time
        if(stopwatchOn == false){
            startTimeMillis = System.currentTimeMillis() - elapsedTimeMillis;
        }

        // update variables and UI
        stopwatchOn = true;
        startStopButton.setText(R.string.stop);

        // if GPS is not enabled, start GPS settings activity
        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "Please activate GPS settings",
                    Toast.LENGTH_SHORT).show();
            Intent intent =
                    new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        // start service
        startService(servicesIntent);
        startNotification();

        // start new timer thread
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis;
                updateViews(elapsedTimeMillis);
            }
        };

        timer = new Timer(true);
        timer.scheduleAtFixedRate(task,0, 100);
    }

    private void updateViews(final long elapsedTimeMillis) {
        elapsedTenths = (int) ((elapsedTimeMillis/100)%10);
        elapsedSecs = (int) ((elapsedTimeMillis/1000)%60);
        elapsedMins = (int) ((elapsedTimeMillis/(60*1000))%60);
        elapsedHours = (int) (elapsedTimeMillis/(60*60*1000));

        if (elapsedHours > 0){
            updateView(hoursTextView, elapsedHours, 1);
        }
        updateView(minsTextView, elapsedMins, 2);
        updateView(secsTextView, elapsedSecs, 2);
        updateView(tenthsTextView, elapsedTenths ,1);

    }

    private void updateView(final TextView textView,
                            final long elapsedTime, final int minIntDigits) {

        // post changes to UI thread
        number = NumberFormat.getInstance();
        textView.post(new Runnable() {
            @Override
            public void run() {
                number.setMinimumIntegerDigits(minIntDigits);
                textView.setText(number.format(elapsedTime));
            }
        });
    }


    private void startNotification() {
        notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent =
                new Intent(this, StopwatchActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, flags);

        int icon = R.drawable.ic_launcher_foreground;
    }

    private void stop() {
        // stop timer
        stopwatchOn = false;
        if(timer != null){
            timer.cancel();
        }
        startStopButton.setText(R.string.start);

        // stop service
        stopService(servicesIntent);
        stopNotification();

        // update views
        updateViews(elapsedTimeMillis);
    }

    private void stopNotification() {
    }
}