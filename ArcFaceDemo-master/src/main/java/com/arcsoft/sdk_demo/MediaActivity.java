package com.arcsoft.sdk_demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import com.firefly.api.FireflyApi;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/*该活动为广告播放系统，同时具备实时检测人脸红外信号的功能，当没有检测到人脸红外信号时，播放视频；
* 当检测到人脸红外信号时，触发DetecterActivity人脸检测活动*/
public class MediaActivity extends Activity implements OnCompletionListener,View.OnClickListener {

    private VideoView videoView;
    private FireflyApi fireflyApi;
    private Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //适配显示器界面
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_media);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        videoView = (VideoView)findViewById(R.id.video_view);
        videoView.setOnCompletionListener(this);
        Button button = (Button)findViewById(R.id.recog_face);
        button.setOnClickListener(this);

        fireflyApi = new FireflyApi();
        //检验SD权限
        if (ContextCompat.checkSelfPermission(MediaActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MediaActivity.this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }else{
            initVideoPath();
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.arg1){
                case 0:
                    try{
                        Intent intent = new Intent(MediaActivity.this,DetecterActivity.class);
                        startActivity(intent);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onClick(View view){
        if (view.getId()==R.id.recog_face){
            Intent intent = new Intent(MediaActivity.this,DetecterActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if (grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    initVideoPath();
                }else {
                    Toast.makeText(this,"拒绝权限将无法使用存储程序",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void initVideoPath(){
        File file = new File(Environment.getExternalStorageDirectory(),"/Video/1.mp4");
        videoView.setVideoPath(file.getPath());//指定视频文件
    }

    @Override
    protected void onStart(){
        super.onStart();
        videoView.requestFocus();
        if (!videoView.isPlaying()){
            videoView.start();//开始播放1.mp4
            thread = new Receive();
            thread.start();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    //当视频1.mp4播放完后，回调该方法，再继续播放
    public void onCompletion(MediaPlayer mp) {
        videoView.requestFocus();
        if (!videoView.isPlaying()){
            videoView.start();//开始播放video
        }
    }

    @Override
    protected void onPause(){
        if (videoView.isPlaying()){
            videoView.pause(); //暂停播放
        }
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (videoView != null){
            videoView.suspend(); //释放视频资源
        }
    }

    //检测人脸活动的线程
    class Receive extends Thread{
        Application application=(Application)MediaActivity.this.getApplicationContext();
        @Override
        public void run(){
            super.run();
            while (true){
                try{
                    String gpio = application.getGpioString("/sys/class/gpio/gpio263/value");
                    int val = Integer.valueOf(gpio);
                    System.out.println("端口的值为：\n");
                    System.out.print(val);
                    Message message = new Message();
                    message.arg1 = val;
                    handler.sendMessage(message);
                    thread.sleep(3000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
