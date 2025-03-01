package com.example.testvideo;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ConnectionManager {
    private static ConnectionManager connectionManager = new ConnectionManager();
    private ServerSocket mServer = null;
    private ServerSocket mControlServer = null;
    private Socket mSocket = null;
    private Socket mControlSocket = null;
    private boolean mRunning = true;
    private ControlDataRec mControlDataRec;

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
                        if (mControlServer == null){
                            mControlServer = new ServerSocket(7777);
                        }
                        if (mServer == null){
                            mServer = new ServerSocket(6666);
                        }
                        mControlSocket = mControlServer.accept();
                        mSocket = mServer.accept();
                        mSocket.getOutputStream().write(66);
                        mSocket.getOutputStream().write(intToByte(mWidth));
                        mSocket.getOutputStream().write(intToByte(mHeight));
                        mControlDataRec = new ControlDataRec(mControlSocket);
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

    Socket getControlSocketSocket(){
        return mControlSocket;
    }

    void endConnect(){
        try {
            mSocket.close();
            mSocket = null;
            if (mControlDataRec != null){
                mControlDataRec.setRunning(false);
            }
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
