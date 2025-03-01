package com.example.projectionclient;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    String ip ="";
    Socket mClientSocket = null;
    Socket mControlSocket = null;
    boolean running = true;
    MediaCodec mediaCodec = null;
    int mDesWidth = 0;
    int mDesHeight = 0;

    Handler mHandler = null;
    int TYPE_MOTION = 0;
    public static int bytesToInt2(byte[] src, int offset){
        int value;
        value = (int) (((src[offset] & 0xFF)<<24)
                |((src[offset+1] & 0xFF)<<16)
                |((src[offset+2] & 0xFF)<<8)
                |(src[offset+3] & 0xFF));
        return value;
    }

    void initMediaCodec(Surface surface, int w, int h){
        try {
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,w,h);
            mediaCodec.configure(format,surface,null,0);
            mediaCodec.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    ArrayList<byte[]> mH264List = new ArrayList<>();
    void decodeH264(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (mediaCodec != null){
                    while (running){
                        byte[] rawH264 = null;
                        synchronized (mH264List){
                            if (mH264List.size() > 0){
                                rawH264 = mH264List.get(0);
                                mH264List.remove(0);
                            }
                            if (rawH264 == null){
                                try {
                                    mH264List.wait();
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                continue;
                            }
                        }

                        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                        int inputIndex = -1;
                        while (inputIndex < 0){
                            inputIndex = mediaCodec.dequeueInputBuffer(100000);
                        }
                        ByteBuffer byteBuffer = inputBuffers[inputIndex];
                        byteBuffer.clear();
                        byteBuffer.put(rawH264);

                        mediaCodec.queueInputBuffer(inputIndex,0,rawH264.length,0,0);

                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outIndex = mediaCodec.dequeueOutputBuffer(info,100000);
                        if (outIndex >= 0){
                            mediaCodec.releaseOutputBuffer(outIndex,true);
                        }
                    }
                }
            }
        }).start();
    }

    float mRatio = -1;
    void caculateRatio(float viewWidth){
        mRatio = (float) mDesWidth / (float) viewWidth;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HandlerThread handlerThread = new HandlerThread("motion");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == TYPE_MOTION){
                    MotionEvent event =(MotionEvent) msg.obj;
                    int action = event.getAction();
                    int x = (int) (event.getX() * mRatio);
                    int y = (int) (event.getY() * mRatio);
                    Log.d("GZG","X:"+x+", Y:"+y);
                    ByteBuffer byteBuffer = ByteBuffer.allocate(13);
                    byteBuffer.put((byte) TYPE_MOTION);
                    byteBuffer.putInt(action);
                    byteBuffer.putInt(x);
                    byteBuffer.putInt(y);
                    byteBuffer.flip();

                    byte[] sendData = new byte[13];
                    byteBuffer.get(sendData);

                    try {
                        mControlSocket.getOutputStream().write(sendData);
                    }catch (Exception e){
                        e.printStackTrace();
                    }


                }
                return false;
            }
        });
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                initMediaCodec(holder.getSurface(),width,height);
                decodeH264();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

          }
        });

        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mRatio < 0){
                    caculateRatio((float) surfaceView.getWidth());
                }
                Message msg = new Message();
                msg.what = TYPE_MOTION;
                msg.obj = MotionEvent.obtain(event);
                mHandler.sendMessage(msg);

                return true;
            }
        });

        EditText editText = findViewById(R.id.editText);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
                // 在这里处理输入的内容
                ip = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        final Button btn = findViewById(R.id.connect);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File saveFile = new File(getExternalCacheDir() + "/test-client.h264");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"saveFile:"+saveFile,Toast.LENGTH_LONG).show();
                            }
                        });


                        try {
                            mControlSocket = new Socket(ip,7777);
                            mClientSocket = new Socket(ip,6666);
                            byte[] data = new byte[1];
                            int len = mClientSocket.getInputStream().read(data);
                            if (len==1 && data[0]==66){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btn.setText("Connect "+ ip + " ok");
                                    }
                                });
                            }
                            byte[] wh = new byte[4];
                            len = mClientSocket.getInputStream().read(wh);
                            if (len == 4){
                                mDesWidth = bytesToInt2(wh,0);
                            }
                            len = mClientSocket.getInputStream().read(wh);
                            if (len == 4){
                                mDesHeight = bytesToInt2(wh,0);
                            }
                            Log.i("GZG","mDesWidth:"+mDesWidth+", mDesHeight:"+mDesHeight);



                            while (running){
//                                byte[] reaData = new byte[1024];
//                                int readLen = mClientSocket.getInputStream().read(reaData);
//                                fileOutputStream.write(reaData,0,readLen);
                                byte[] headLen = new byte[4];
                                int readLen = 0;
                                while (readLen < headLen.length){
                                    readLen = readLen + mClientSocket.getInputStream().read(headLen
                                            ,readLen,headLen.length-readLen);
                                }
                                //把 byte[] -> int
                                int h264Len = bytesToInt2(headLen,0);
                                byte[] h264Data = new byte[h264Len];
                                int readLen1 = 0;
                                while (readLen1 < h264Len){
                                    readLen1 = readLen1 + mClientSocket.getInputStream().read(h264Data
                                            ,readLen1,h264Len-readLen1);
                                }
                                //fileOutputStream.write(h264Data,0,readLen1);
                                synchronized (mH264List){
                                    mH264List.add(h264Data);
                                    mH264List.notify();
                                }


                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });
    }
}