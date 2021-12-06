package com.example.timer_sync;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button btnSwitch, btnFinish, btnStartStop;
    TextView viewTimer, viewExtraTime;

    long timeLeft;
    boolean isTimerRun, isFinish, alreadyAdd2min;
    final int EIGHT_MIN =  60 * 8  , TEN_MIN = 60  * 10, TWO_MIN =  60 * 2;

    CountDownTimer countDownTimer;

    Vibrator vibrator;
    AlertDialog.Builder builder;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("STAT");
    DataSnapshot dataSnapshot;

    boolean connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkConnection();

        btnSwitch = findViewById(R.id.switch_btn);
        btnFinish = findViewById(R.id.finish_btn);
        btnStartStop = findViewById(R.id.start_btn);
        viewTimer = findViewById(R.id.viewTime);
        viewExtraTime = findViewById(R.id.viewExtraTime);

        btnSwitch.setOnClickListener(this);
        btnFinish.setOnClickListener(this);
        btnStartStop.setOnClickListener(this);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        builder = new AlertDialog.Builder(this);

        myRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint({"DefaultLocale", "SetTextI18n"})
            @Override
            public void onDataChange(DataSnapshot data) {

                checkConnection();

                dataSnapshot = data;

                isTimerRun = Boolean.parseBoolean(getValueByKeyDB("isTimerRun"));
                isFinish = Boolean.parseBoolean(getValueByKeyDB("isFinish"));
                alreadyAdd2min = Boolean.parseBoolean(getValueByKeyDB("alreadyAdd2min"));

                btnSwitch.setEnabled(Boolean.parseBoolean(getValueByKeyDB("isSwitchEnabled")));
                btnStartStop.setEnabled(Boolean.parseBoolean(getValueByKeyDB("isStartStopEnabled")));
                btnFinish.setEnabled(Boolean.parseBoolean(getValueByKeyDB("isFinishEnabled")));

                viewExtraTime.setVisibility(Boolean.parseBoolean(getValueByKeyDB("isExtraVisibility")) ? View.VISIBLE : View.INVISIBLE);
                btnSwitch.setText(getValueByKeyDB("switchTime"));
                // 1000 ms = 1 sec , 1000 * 60 ms = 1 min
                timeLeft = Integer.parseInt(getValueByKeyDB("timeLeft"));
                viewTimer.setText(String.format("%02d:%02d", timeLeft / 60  % 60, timeLeft  % 60));

                if (btnStartStop.getText().equals("Start") && isTimerRun) {     //  isTimerRun ture was update but the text no
                    setValueByKeyDB("isSwitchEnabled", false);
                    startTimer();
                    btnStartStop.setText("Stop");
                } else if (btnStartStop.getText().equals("Stop") && !isTimerRun) {
                    btnStartStop.setText("Start");
                    cancelTimer();

                }

                if (isFinish) {
                    timeLeft = btnSwitch.getText().equals("switch to 8' min") ? TEN_MIN
                            : EIGHT_MIN;
                    setValueByKeyDB("timeLeft", timeLeft);

                    setValueByKeyDB("isTimerRun", false);
                    btnStartStop.setText("Start");
                    setValueByKeyDB("alreadyAdd2min", false);

                    setValueByKeyDB("isFinishEnabled", false);
                    setValueByKeyDB("isSwitchEnabled", true);
                    setValueByKeyDB("isStartStopEnabled", true);
                    setValueByKeyDB("isExtraVisibility", false);

                    cancelTimer();

                } else {
                    setValueByKeyDB("isFinishEnabled", true);
                }

                if (timeLeft < 2 && !alreadyAdd2min) {
                    vibrator.vibrate(1000);
                    setValueByKeyDB("alreadyAdd2min", true);
                    cancelTimer();

                    timeLeft = TWO_MIN;
                    setValueByKeyDB("timeLeft", timeLeft);
                    startTimer();

                    setValueByKeyDB("isExtraVisibility", true);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    @SuppressLint("NonConstantResourceId")
    public void onClick(View v) {
        Button btn = (Button) v;
        switch (btn.getId()) {
            case R.id.finish_btn:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            setValueByKeyDB("isFinish", true);
                        }
                    }
                };
                builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                break;
            case R.id.switch_btn:
                String temp = btnSwitch.getText().equals("switch to 8' min") ? "switch to 10' min"
                        : "switch to 8' min";
                setValueByKeyDB("switchTime", temp);
                timeLeft = temp.equals("switch to 10' min") ? EIGHT_MIN : TEN_MIN;
                setValueByKeyDB("timeLeft", timeLeft);
                break;

            case R.id.start_btn:                        // stop or strat timer and update DB
                isTimerRun = !isTimerRun;
                setValueByKeyDB("isFinish", false);
                setValueByKeyDB("isTimerRun", isTimerRun);
                break;

        }
    }

    public void startTimer() {
        countDownTimer = new CountDownTimer(timeLeft *1000, 1000) {

            public void onTick(long millisUntilFinished) {
                checkConnection();
                timeLeft = millisUntilFinished/1000;
                setValueByKeyDB("timeLeft", timeLeft);

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                vibrator.vibrate(1000);
                setValueByKeyDB("timeLeft", 0);
                setValueByKeyDB("isStartStopEnabled", false);
            }

        }.start();
    }

    public String getValueByKeyDB(String key) {
        return dataSnapshot.child(key).getValue().toString();
    }

    public void setValueByKeyDB(String key, Object obj) {
        myRef.child(key).setValue(obj);
    }

    public void cancelTimer() {
        try {
            countDownTimer.cancel();
        } catch (NullPointerException ignored) {
        }
    }

    public void checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //we are connected to a network?
        connected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED;

        if (!connected) {
            Toast.makeText(this, "No Internet connection", Toast.LENGTH_LONG).show();
            cancelTimer();
            moveTaskToBack(false);
            android.os.Process.killProcess(android.os.Process.myPid());
            finish();

        }
    }

}