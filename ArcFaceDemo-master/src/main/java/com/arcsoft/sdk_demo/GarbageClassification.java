package com.arcsoft.sdk_demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.api.client.http.HttpResponse;
import com.jdcloud.apigateway.signature.JdcloudSDKClient;
import com.jdcloud.sdk.http.Protocol;
import com.jdcloud.sdk.utils.BinaryUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import Decoder.BASE64Encoder;

import static com.jdcloud.sdk.http.Protocol.HTTP;
import static com.jdcloud.sdk.http.Protocol.HTTPS;

//点击按钮拍摄垃圾图片，并调用后台API进行识别，并对识别结果进行解析
public class GarbageClassification extends AppCompatActivity implements View.OnClickListener {

    public static final int TAKE_PHOTO = 1;
    private String accessKey = "E3DB92C9C4321C8E8CF278C22A7B292A";
    private String secretKey = "62C9388EDB679D943CEE75E0FA7D7157";
    private String endPoint = "aiapi.jdcloud.com";
    private String path = "/jdai/garbageImageSearch";
    private String method = "POST";
    private String jsonStr;
    private ImageView imageView;
    private Uri imageUri;
    private Bitmap bitmap;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_garbage_classification);
        imageView = (ImageView)findViewById(R.id.picture);
        Button takephoto = (Button)findViewById(R.id.take_photo);
        takephoto.setOnClickListener(this);
        textView = (TextView)findViewById(R.id.text);
    }

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {    //获取消息，更新UI
                case 1:
                    String txt = (String)msg.obj;
                    textView.setText(txt);
                    break;
            }
        }
    };

    @Override
    public void onClick(View view){
        File outputImage= new File(getExternalCacheDir(),"output_image.jpg");
        try{
            if(outputImage.exists()) {
                outputImage.delete();
            }
            boolean bool=outputImage.createNewFile();
            if(bool){
                Log.d("缓存路径","设置成功");
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT>=24){
            imageUri=FileProvider.getUriForFile(GarbageClassification.this,"com.example.cameraalbumtest.fileprovider",outputImage);
        }else{
            imageUri = Uri.fromFile(outputImage);
        }
        //启动相机程序
        Intent takePhotoIntent = new Intent("android.media.action.IMAGE_CAPTURE");  //用来打开相机的Intent
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);            //设置照片存储路径
        if(takePhotoIntent.resolveActivity(getPackageManager())!=null){        //这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            startActivityForResult(takePhotoIntent,TAKE_PHOTO);                  //启动相机
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case TAKE_PHOTO:
                if(resultCode==RESULT_OK){
                    try{
                        Bitmap mbitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                        imageView.setImageBitmap(mbitmap);
                        //修改图片尺寸
                        bitmap = imageScale(mbitmap,2000,800);
                        if (bitmap!=null){
                            new RecognizeGarbage(Bitmap2Bytes(bitmap)).start();//开启线程将垃圾图片上传到服务器进行识别
                            //garbage(Bitmap2Bytes(bitmap));
                        }else {
                            Log.d("修改尺寸","为空！");
                        }

                    }catch (FileNotFoundException e){
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    class RecognizeGarbage extends Thread{

        byte[] bytes;
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> queryMap = new HashMap<>();

        public RecognizeGarbage(byte[] by){
            bytes = by;
            headers.put("x-jdcloud-algorithm","JDCLOUD2-HMAC-SHA256");
            headers.put("x-jdcloud-nonce","ed558a3b-9808-4edb-8597-187bda63a4f2");
        }

        @Override
        public void run(){
            System.out.println("图片识别线程开启成功！");
            BASE64Encoder encoder = new BASE64Encoder();
            String image_base64 = encoder.encode(bytes);
            String str = "{\"cityId\":\"110000\",\"imgBase64\":\"";
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(str);
            stringBuffer.append(image_base64);
            stringBuffer.append("\"}");
            String body = stringBuffer.toString();
            try{
                HttpResponse response = JdcloudSDKClient.execute(accessKey, secretKey,Protocol.HTTP,endPoint,path, method, headers, queryMap, body);
                jsonStr = new String(BinaryUtils.toByteArray(response.getContent()));
                System.out.println(jsonStr);
                String result = JsonToResult(jsonStr);
                if (result!=null){
                    System.out.println(result);
                }else{
                    System.out.println("result为空！");
                }
                Message message = new Message();
                message.what= 1;
                message.obj = result;
                mHandler.sendMessage(message);

            }catch (IOException e){
                e.printStackTrace();
            }finally {
                System.out.println("图片识别成功");
            }
        }
    }

    /**
     * 调整图片大小
     * @param bitmap 源
     * @param dst_w 输出宽度
     * @param dst_h 输出高度
     * @return
     */
    public Bitmap imageScale(Bitmap bitmap, int dst_w, int dst_h) {
        int src_w = bitmap.getWidth();
        int src_h = bitmap.getHeight();
        float scale_w = ((float) dst_w) / src_w;
        float scale_h = ((float) dst_h) / src_h;
        Matrix matrix = new Matrix();
        matrix.postScale(scale_w, scale_h);
        Bitmap dstbmp = Bitmap.createBitmap(bitmap, 0, 0, src_w, src_h, matrix,
                true);
        return dstbmp;
    }

    public byte[] Bitmap2Bytes(Bitmap bm){
        if (bm==null){
            Log.d("bm","为空！");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean bool= bm.compress(CompressFormat.PNG, 100, baos);
        if (bool){
            return baos.toByteArray();
        }
        return null;
    }

    public String JsonToResult(String string){
        JSONObject jsonObject = JSON.parseObject(string);
        JSONObject jsonObject1 = jsonObject.getJSONObject("result");
        JSONArray jsonArray = jsonObject1.getJSONArray("garbage_info");
        String res = null;
        for(int i=0;i<jsonArray.size();i++){
            JSONObject jsonObject2 = jsonArray.getJSONObject(i);
            float confidence = jsonObject2.getFloat("confidence");
            if (confidence>=0.2){
                String garbage_name = jsonObject2.getString("garbage_name");
                float confident = jsonObject2.getFloat("confidence");
                res ="name：" + garbage_name + " confidence："+ Float.toString(confident);
                System.out.println("结果为：");
                System.out.println(res);
                System.out.println(jsonObject2.getFloat("confidence"));//得到置信度大于0.5的数据
            }
        }
        return res;
    }
}
