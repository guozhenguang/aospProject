package com.example.testvideo;

import java.net.Socket;
public class ControlDataRec {
    int TYPE_MOTION = 0;
    Socket controlSocket = null;
    boolean mRunning = true;

    public int bytesToInt2(byte[] src, int offset){
        int value;
        value = (int) (((src[offset] & 0xFF)<<24)
                |((src[offset+1] & 0xFF)<<16)
                |((src[offset+2] & 0xFF)<<8)
                |(src[offset+3] & 0xFF));
        return value;
    }

    public void setRunning(boolean running){
        mRunning = running;
    }


    ControlDataRec(Socket socket){
        controlSocket = socket;
        if (controlSocket != null){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mRunning){
                        try {
                            byte[] type = new byte[1];
                            int len = 0;
                            len = controlSocket.getInputStream().read(type);
                            if (len != 1){
                                continue;
                            }else {
                                if (type[0] == TYPE_MOTION){
                                    byte[] data = new byte[4];
                                    len = 0;
                                    while (len < 4){
                                        len = len + controlSocket.getInputStream().read(data,len,data.length-len);
                                    }
                                    int action = bytesToInt2(data,0);

                                    len = 0;
                                    while (len < 4){
                                        len = len + controlSocket.getInputStream().read(data,len,data.length-len);
                                    }
                                    int x = bytesToInt2(data,0);

                                    len = 0;
                                    while (len < 4){
                                        len = len + controlSocket.getInputStream().read(data,len,data.length-len);
                                    }
                                    int y = bytesToInt2(data,0);
                                    InjectUtil.injectTouch(action,x,y);
                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
}
