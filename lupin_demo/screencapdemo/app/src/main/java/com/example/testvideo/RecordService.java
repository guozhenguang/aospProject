package com.example.testvideo;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class RecordService extends Service {
    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;
    private float refreshRate;

    private boolean running;

    DisplayMetrics dm = RecordApplication.getInstance().getResources().getDisplayMetrics();
    private int width = dm.widthPixels;
    private int height = dm.heightPixels;
//    private int width = 480;
//    private int height = 640;
    private int dpi = dm.densityDpi;


    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d("GZG","RecordService onCreate");
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
        mediaRecorder = new MediaRecorder();
    }

    public void setRate(float rate){
        refreshRate = rate;
    }


    public void startForeground(){
        Log.d("GZG","RecordService startForeground");
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Screencap服务 has started");

        NotificationChannel channel = new NotificationChannel("screen_cap","Screencap", NotificationManager.IMPORTANCE_HIGH);
        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        notificationBuilder.setChannelId("screen_cap");
        Notification notification = notificationBuilder.build();
        startForeground(1,notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    private boolean isRecording = false;

    public boolean startRecord() {
        if (mediaProjection == null || running) {
            return false;
        }
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                initMediaCodec();
            }
        }).start();

        return true;
    }

    private void initMediaCodec() {
        initRecorder();
//        try {
//            mediaRecorder.prepare();
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }
//        createVirtualDisplay(mediaRecorder.getSurface());
//        mediaRecorder.start();
    }

    public boolean stopRecord() {
        if (!running ) {
            return false;
        }
        running = false;
        if (virtualDisplay != null){
            virtualDisplay.release();
        }
        mediaProjection.stop();

        return true;
    }


    private void createVirtualDisplay(Surface surface) {
        mediaProjection.registerCallback(new MediaProjection.Callback(){},new Handler(Looper.getMainLooper()));
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null);
    }

    public String getsaveDirectory() {
        Log.d("GZG","getExternalStorageState:"+Environment.getExternalStorageState());
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.d("GZG","getsaveDirectory");
            String rootDir = RecordApplication.getInstance().getExternalCacheDir() + File.separator
                    + "ScreenRecord" +  File.separator ;
            Log.d("GZG","Dir:"+rootDir);

            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }

            return rootDir;
        } else {
            Log.d("GZG","no getsaveDirectory");
            return null;
        }
    }

    private static final int DEFAULT_I_FRAME_INTERVAL = 10; //conds


    private static MediaFormat createFormal(int bitRate, float frameRate){
        Log.d("GZG","frameRate："+frameRate);
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME,MediaFormat.MIMETYPE_VIDEO_AVC);
        format.setInteger(MediaFormat.KEY_BIT_RATE,bitRate);
        format.setFloat(MediaFormat.KEY_FRAME_RATE,frameRate);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,DEFAULT_I_FRAME_INTERVAL);

        // 添加性能优化参数
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel52);
        format.setInteger(MediaFormat.KEY_LATENCY, 1); // 低延迟模式

        return format;
    }
    private static void setSize(MediaFormat format, int width, int height){
        format.setInteger(MediaFormat.KEY_WIDTH,width);
        format.setInteger(MediaFormat.KEY_HEIGHT,height);
    }

    private static void configure(MediaCodec codec, MediaFormat format){
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    void initRecorder() {
        int baseBitrate = 8000000;
        int adjustedBitrate = (int) (baseBitrate * (refreshRate / 60f));
        MediaFormat format = createFormal(adjustedBitrate, refreshRate);

        //MediaFormat format = createFormal(8000000);
        boolean alive = false;
        try {
            do{
                MediaCodec codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                setSize(format,width,height);
                configure(codec,format);
                Surface surface = codec.createInputSurface();
                createVirtualDisplay(surface);
                codec.start();
                try {
                    alive = encode(codec,getsaveDirectory());
                    codec.stop();
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    codec.release();
                    surface.release();
                }
            }while (alive);
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }
    }

    File h264File = null;
    FileOutputStream  fileOutputStream = null;
    private boolean encode(MediaCodec codec, String path) throws IOException{
        if (h264File == null){
            h264File = new File(path + "test.h264");
            fileOutputStream = new FileOutputStream(h264File);
        }
        boolean eof = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (running && !eof){
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, -1);
            eof = (bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            try{
                if (outputBufferId >= 0){
                    ByteBuffer codecBuffer = codec.getOutputBuffer(outputBufferId);
                    byte[] data = new byte[codecBuffer.remaining()];
                    codecBuffer.get(data);

                    //把int-> byte[]
                    int h264Len = data.length;
                    ByteBuffer dataLenBuf = ByteBuffer.allocate(4);
                    dataLenBuf.putInt(h264Len);
                    dataLenBuf.flip();

                    byte[] dataLen = new byte[4];
                    dataLenBuf.get(dataLen);

                    if (ConnectionManager.getInstance().getSocket() != null){
                        ConnectionManager.getInstance().getSocket().getOutputStream().write(dataLen);
                        ConnectionManager.getInstance().getSocket().getOutputStream().write(data);
                    }

       //             fileOutputStream.write(data);
                }
            }finally {
                if (outputBufferId >= 0){
                    codec.releaseOutputBuffer(outputBufferId,false);
                }
            }
        }
        if (ConnectionManager.getInstance().getSocket() != null){
            ConnectionManager.getInstance().endConnect();
        }

        if (fileOutputStream != null){
            fileOutputStream.flush();
            fileOutputStream.close();
            h264File = null;
            fileOutputStream = null;
        }
        return !eof && running;
    }



    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }
}