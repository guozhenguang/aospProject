package com.example.remotesubmix;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private static final int AUDIO_SOURCE= MediaRecorder.AudioSource.REMOTE_SUBMIX;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLE_RATE = 44100;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("GZG","rt record");
                startRecord();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAudioRecord();
    }

    AudioRecord audioRecord;
    int bufferSize = 0;
    FileOutputStream fileOutputStream = null;
    File saveFile = null;
    boolean mRecording = true;
    void startRecord(){
        mRecording = true;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,CHANNEL,AUDIO_FORMAT);
        audioRecord = new AudioRecord(AUDIO_SOURCE,SAMPLE_RATE,CHANNEL,AUDIO_FORMAT,bufferSize);
        audioRecord.startRecording();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mRecording){
                    byte[] buffer = new byte[bufferSize];
                    int len = audioRecord.read(buffer,0,buffer.length);
                    if (len > 0){
                        try {
                            if (saveFile == null) {
                                saveFile = new File(MainActivity.this.getExternalCacheDir() + "/remoutesubmix.pcm");
                                fileOutputStream = new FileOutputStream(saveFile);
                            }

                            if (fileOutputStream != null) {
                                fileOutputStream.write(buffer, 0, len);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    void stopAudioRecord(){
        if (audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
        }
        mRecording = false;
    }
}