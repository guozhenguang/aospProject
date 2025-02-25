package com.example.testvideo;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ConnectionManager {
    private static ConnectionManager connectionManager = new ConnectionManager();
    private ServerSocket mServer = null;
    private Socket mSocket = null;
    private boolean mRunning = true;

    private ConnectionManager(){}

    public static ConnectionManager getInstance(){
        return connectionManager;
    }

    void init(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mRunning){
                    try {
                        if (mServer == null){
                            mServer = new ServerSocket(6666);
                        }
                        mSocket = mServer.accept();
                        mSocket.getOutputStream().write(66);
                        mSocket.getOutputStream().write(intToByte(mWidth));
                        mSocket.getOutputStream().write(intToByte(mHeight));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    Socket getSocket(){
        return mSocket;
    }

    void endConnect(){
        try {
            mSocket.close();
            mSocket = null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    int mWidth = 0;
    int mHeight = 0;
    void setWidthAndHeight(int w, int h){
        mWidth = w;
        mHeight = h;
    }

    byte[] intToByte(int input){
        ByteBuffer dataLenBuf = ByteBuffer.allocate(4);
        dataLenBuf.putInt(input);
        dataLenBuf.flip();
        byte[] data = new byte[4];
        dataLenBuf.get(data);
        return data;
    }
}
