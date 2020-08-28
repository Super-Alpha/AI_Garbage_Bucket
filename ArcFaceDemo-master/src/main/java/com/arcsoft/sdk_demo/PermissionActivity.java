package com.arcsoft.sdk_demo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

//import com.arcsoft.sdk_demo.DataBase.ImageDemo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gqj3375 on 2017/2/24.
 */
public class PermissionActivity extends Activity {

	public static int PERMISSION_REQ = 0x123456;
	private String[] mPermission = new String[] {
			Manifest.permission.INTERNET,
			Manifest.permission.CAMERA,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
	};
	private List<String> mRequestPermission = new ArrayList<String>();
	/*
	 *(non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what==1){
				startActiviy();
			}
		}
	};
	@Override
	protected void onCreate(Bundle savedInstanceState){
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		//开启自动注册的网络后台服务
		Intent intent = new Intent(PermissionActivity.this,AutoRegister_and_SaveVideo.class);
		startService(intent);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			for (String one : mPermission) {
				if (PackageManager.PERMISSION_GRANTED != this.checkPermission(one, Process.myPid(), Process.myUid())) {
					mRequestPermission.add(one);
				}
			}
			if (!mRequestPermission.isEmpty()) {
				this.requestPermissions(mRequestPermission.toArray(new String[mRequestPermission.size()]), PERMISSION_REQ);
				return ;
			}
		}
		startActiviy();
	}
	public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
		// 版本兼容
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
			return;
		}
		if (requestCode == PERMISSION_REQ) {
			for (int i = 0; i < grantResults.length; i++) {
				for (String one : mPermission) {
					if (permissions[i].equals(one) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
						mRequestPermission.remove(one);
					}
				}
			}
			startActiviy();
		}
	}
	//当MainActivity结束后，回调该方法，
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PERMISSION_REQ) {
			if (resultCode == 0) {
				this.finish(); //结束本活动
			}
		}
	}
	public void startActiviy() {
		if (mRequestPermission.isEmpty()) {
			final ProgressDialog mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgressDialog.setTitle("loading register data...");
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
			new Thread(new Runnable() {
				@Override
				public void run() {
					Application app = (Application) PermissionActivity.this.getApplicationContext();
					app.mFaceDB.loadFaces();
					PermissionActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Intent intent = new Intent(PermissionActivity.this, MediaActivity.class);
							startActivityForResult(intent, PERMISSION_REQ);
						}
					});
				}
			}).start();
		} else {
			Toast.makeText(this, "PERMISSION DENIED!", Toast.LENGTH_LONG).show();
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					PermissionActivity.this.finish();
				}
			}, 3000);
		}
	}
}
