package com.arcsoft.sdk_demo;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Environment;
import android.os.IBinder;
import android.os.MemoryFile;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;

import org.json.JSONException;
import org.json.JSONObject;

import Decoder.BASE64Decoder;

public class AutoRegister_and_SaveVideo extends Service {

    public void onCreate(){
        super.onCreate();
    }

    public int onStartCommand(Intent intent,int flags,int startId){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //接收服务器发过来的图片信息
                try {
                    //创建Socket
                    Socket socket = new Socket("192.168.1.55", 12345); //IP：电脑作为服务器，端口可设置为任意值，但要与服务器端的端口值相同
                    //接收来自服务器的消息
                    String strInputstream = "";
                    InputStream inputStream = socket.getInputStream();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] by = new byte[2048];
                    int n;
                    while((n=inputStream.read(by))!=-1){
                        baos.write(by,0,n);
                    }
                    strInputstream = new String(baos.toByteArray());
                    System.out.println("接受到的数据长度为：" + strInputstream);
                    socket.shutdownInput();
                    inputStream.close();
                    baos.close();

                    //处理服务器端数据，将socket接收到的数据还原为JSONObject，并注册！
                    try{
                        JSONObject json = new JSONObject(strInputstream);
                        String id = json.getString("id");
                        String imgStr = json.getString("img");
                        RegisterUser(id,imgStr);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }finally {
                        Log.d("人脸","注册成功");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    Log.d("服务器","数据接收成功");
                }
            }
        }).start();
        return super.onStartCommand(intent,flags,startId);
    }

    public IBinder onBind(Intent intent){
        throw new UnsupportedOperationException("");
    }

    //对用户信息进行注册
    public void RegisterUser(String id,String image){

        BASE64Decoder decoder = new BASE64Decoder();

        AFR_FSDKEngine engine = new AFR_FSDKEngine();

        AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
        Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error.getCode());

        //对字符图片进行解码
        try{
            byte[] bytes = decoder.decodeBuffer(image);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);

            AFR_FSDKFace face = new AFR_FSDKFace();

            //提取脸部特征
            error = engine.AFR_FSDK_ExtractFRFeature(bytes,bitmap.getWidth(), bitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(210,178,478,446),AFR_FSDKEngine.AFR_FOC_0,face);
            Log.d("com.arcsoft", "Face=" + face.getFeatureData()[0]+ "," + face.getFeatureData()[1] + "," + face.getFeatureData()[2] + "," + error.getCode());

            //对人脸进行注册
            ((Application)AutoRegister_and_SaveVideo.this.getApplicationContext()).mFaceDB.addFace(id, face,bitmap);//对获得的数据进行注册

        }catch (IOException e){
            e.printStackTrace();
        }finally {
            //销毁人脸识别引擎，释放内存
            error = engine.AFR_FSDK_UninitialEngine();
            Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error.getCode());
        }
    }
}
