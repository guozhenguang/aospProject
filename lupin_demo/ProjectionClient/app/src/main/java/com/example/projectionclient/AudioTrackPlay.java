package com.example.projectionclient;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import java.net.Socket;


public class AudioTrackPlay {
    private static final int AUDIO_SOURCE= MediaRecorder.AudioSource.REMOTE_SUBMIX;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int SAMPLE_RATE = 44100;
    private static AudioTrackPlay audioTrackPlay = new AudioTrackPlay();
    int bufferSize = 0;
    AudioTrack mAudioTrack = null;
    boolean mRunning = true;

    private AudioTrackPlay(){
        bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE,CHANNEL,AUDIO_FORMAT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,SAMPLE_RATE,CHANNEL,AUDIO_FORMAT,
                bufferSize,AudioTrack.MODE_STREAM);

    }

    public int getBufferSize(){
        return bufferSize;
    }

    void playAudio(byte[] data, int len){
        if (mAudioTrack != null){
            mAudioTrack.play();
            mAudioTrack.write(data,0,len);
        }
    }

    public static AudioTrackPlay getInstance(){
        return audioTrackPlay;
    }

    void receiveData(Socket socket){
        if (socket != null){
            mRunning = true;
            byte[] buf = new byte[bufferSize];
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mRunning) {
                        try {
                            int len = socket.getInputStream().read(buf);
                            playAudio(buf, len);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    void stopReceive(){
        mRunning = false;
    }
}
