package com.arcsoft.sdk_demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firefly.api.FireflyApi;
import com.firefly.api.utils.LogUtil;
import com.mysql.cj.util.LogUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.arcsoft.sdk_demo.PermissionActivity.PERMISSION_REQ;

/*该活动以图片作为背景界面，当检测到SD中视频存在时，转到MediaActivity活动*/
public class MainActivity extends Activity implements View.OnClickListener {

	private final String TAG = this.getClass().toString();
	private FireflyApi fireflyApi;
	private boolean occupied;
	private int gpio;
	private Thread thread;
	private TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main_test);
		fireflyApi = new FireflyApi();
		Button button = (Button)findViewById(R.id.start_face);
		button.setOnClickListener(this);
		textView = (TextView)findViewById(R.id.text);

    }

    Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			String res = (String)msg.obj;
			textView.setText(res);
			switch (msg.arg1){
				case 0:
					try{
						Intent intent = new Intent(MainActivity.this,MediaActivity.class);
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
		if (view.getId()==R.id.start_face){
			Intent intent = new Intent(MainActivity.this,MediaActivity.class);
			startActivity(intent);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		thread = new ReceiveSignal();
		thread.start();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		thread.interrupt();
	}


	//检测人脸活动的线程
	class ReceiveSignal extends Thread{
		@Override
		public void run(){
			super.run();
			String path="/sys/class/gpio/gpio263/value";
			String result="";
			Application app = (Application) MainActivity.this.getApplicationContext();
			while (true){
				//红外传感器监测到变化，则触发多媒体播放活动
				try{
					result = app.getGpioString(path);
					System.out.println("gpio的值：\n");
					System.out.println(result);
					Message message = new Message();
					message.arg1 = Integer.valueOf(result);
					message.obj  = result;
					handler.sendMessage(message);
					thread.sleep(3000);
				}catch (Exception e){
					e.printStackTrace();
				}
			}
		}
	}
}

