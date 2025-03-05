package com.example.testvideo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.net.Socket;


public class AudioDataManager {

    private static final int AUDIO_SOURCE= MediaRecorder.AudioSource.REMOTE_SUBMIX;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLE_RATE = 44100;

    private static AudioDataManager sAudioDataManager = new AudioDataManager();

    private AudioDataManager(){}

    public static AudioDataManager getInstance(){
        return sAudioDataManager;
    }

    AudioRecord audioRecord;
    int bufferSize = 0;
    boolean mRecording = true;
    void startRecord(Socket socket){
        mRecording = true;
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,CHANNEL,AUDIO_FORMAT);
        audioRecord = new AudioRecord(AUDIO_SOURCE,SAMPLE_RATE,CHANNEL,AUDIO_FORMAT,bufferSize);
        audioRecord.startRecording();
        new Thread(() -> {
            while (mRecording){
                byte[] buffer = new byte[bufferSize];
                int len = audioRecord.read(buffer,0,buffer.length);
                if (len > 0){
                    try {
                        if (socket != null){
                            socket.getOutputStream().write(buffer,0,len);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    void stopAudioRecord(){
        if (audioRecord != null){
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        mRecording = false;
    }
}
