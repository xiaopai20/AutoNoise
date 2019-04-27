package com.example.autonoise;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private boolean started = true;
    private MediaPlayer mp = new MediaPlayer();;
    private MediaRecorder recorder = null;

    private final int SOUND_THRESHOLD = 5000;
    private int playDuration = 60 * 1000;
    private long startTime = 0;
    private long stopTime = 0;
    private int lastSoundLevel = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.log("test");

        EditText textMinutes = findViewById(R.id.minutes);
        textMinutes.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
                setMinutes();
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                setMinutes();
            }
        });

        try{
            AssetFileDescriptor afd = getAssets().openFd("raw/White-Noise.mp3");

            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mp.prepare();
            mp.setLooping(true);

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile("/dev/null");
            recorder.prepare();
            recorder.start();

            final Handler handler = new Handler();

            Timer timerObj = new Timer();
            TimerTask timerTaskObj = new TimerTask() {
                public void run() {
                    handler.post(new Runnable() {
                        public void run() {
                            timerRun();
                        }

                    });
                }
            };
            timerObj.schedule(timerTaskObj, 0, 1000);

        }catch(IOException e){
            this.log(e.getMessage());
        }
    }

    private void setMinutes() {
        EditText textMinutes = findViewById(R.id.minutes);
        try {
            playDuration = Integer.parseInt(textMinutes.getText().toString()) * 1000 * 60;
            this.log("Set minutes to stop: " + textMinutes.getText());
        } catch (Exception e) {
            this.log(e.getMessage());
        }
    }

    private void timerRun() {
        int soundLevel = recorder.getMaxAmplitude();
        this.log("started: " + started + "; sound level: " + soundLevel + "; playing:" + mp.isPlaying() + ";");

        if (System.currentTimeMillis() < stopTime + 2000) {
            this.log("just stopped, skip...");
            return;
        }

        try{
            if (!started) {
                stop();
            } else {
                if (!mp.isPlaying() && soundLevel > SOUND_THRESHOLD && lastSoundLevel > SOUND_THRESHOLD) {
                    startTime = System.currentTimeMillis();
                    play();
                } else {
                    if (System.currentTimeMillis() > startTime + playDuration) {
                        stop();
                    }
                }
            }
        }catch(Exception e){
            this.log(e.getMessage());
        }
        lastSoundLevel = soundLevel;
    }

    private void play() {
        if (!mp.isPlaying()) {
            this.log("playing...");
            mp.start();
        }
    }

    private void stop() {
        if (mp != null && mp.isPlaying()) {
            this.log("stopping...");
            stopTime = System.currentTimeMillis();
            mp.pause();
        }
    }

    public void onClickButtonStart(View v) {
        started = !started;
        if (started) {
            Button btn = v.findViewById(R.id.buttonStart);
            btn.setText("Stop");
        } else {
            Button btn = v.findViewById(R.id.buttonStart);
            btn.setText("Start");
        }
    }

    private void log(String msg) {
        TextView textView = this.findViewById(R.id.textLog);
        textView.append("\n" + msg);
    }
}
