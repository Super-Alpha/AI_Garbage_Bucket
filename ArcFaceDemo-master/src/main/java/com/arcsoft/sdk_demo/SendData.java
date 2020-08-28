package com.arcsoft.sdk_demo;

import android.util.Log;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import com.firefly.api.serialport.SerialPort;

public class SendData extends Thread implements SerialPort.Callback{

    private static final String TAG = "SendData";
    private static final String SERIAL_PORT_PATH = "/dev/ttyS3";
	private static final int SERIAL_PORT_BAUDRATE = 9600;
    private SerialPort mSerialPort=null;
    private Boolean mboolean;
    private String servername =" 172.25.11.115";
    private int port = 9999;
    byte[] buffer = new byte[64];

    @Override
    public void run(){
        mboolean=openSerialPort(SERIAL_PORT_PATH,SERIAL_PORT_BAUDRATE);
        if (mboolean){
            Log.d("Open Serial","Successed!!!");
            this.onDataReceived(buffer,64);
        }
    }

    private boolean openSerialPort(String path,int baudrate)
    {
        if(mSerialPort != null)mSerialPort.closeSerialPort();
        mSerialPort = null;
        try {
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
            mSerialPort.setCallback(this);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(TAG, "open serialport("+path +") error:"+e.toString());
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.d(TAG, "open serialport("+path +") error:"+e.toString());
            return false;
        }
        return true;
    }

    //调用此方法即可得到串口中的数据并发送到服务器端
    @Override
    public void onDataReceived(final byte[] buffer,final int size){
        final String result=new String(buffer,0,size);
        Log.d(TAG,"onDataReceived:"+ result);
        if (result!=null && result.trim().length()>0){
            //开启线程，通过socket发送数据到服务器端
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        Socket client = new Socket(servername,port);
                        OutputStream outToServer = client.getOutputStream();
                        DataOutputStream out = new DataOutputStream(outToServer);
                        out.write(buffer,0,size);

                        InputStream inFromServer = client.getInputStream();
                        DataInputStream in = new DataInputStream(inFromServer);
                        Log.d("收到服务器发送回来的数据","Data = "+in.readUTF());
                        client.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
