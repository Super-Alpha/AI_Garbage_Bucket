package com.arcsoft.sdk_demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.firefly.api.FireflyApi;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by gqj3375 on 2017/4/28.
 */
public class Application extends android.app.Application {
	//当应用程序一开启，首先进行Application，其从一开始到结束都在运行
	private final String TAG = this.getClass().toString();
	FaceDB mFaceDB;
	Uri mImage;
	File mfile;
	int count=1;

	@Override
	public void onCreate() {
		super.onCreate();
		mFaceDB = new FaceDB(this.getExternalCacheDir().getPath());
		mImage = null;
		Build_File();
	}

	public void setCaptureImage(Uri uri) {
		mImage = uri;
	}
	public Uri getCaptureImage() {
		return mImage;
	}
	public int getCount(){return count;}
	/**
	 * @param path
	 * @return
	 */
	public static Bitmap decodeImage(String path) {
		Bitmap res;
		try {
			ExifInterface exif = new ExifInterface(path);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			BitmapFactory.Options op = new BitmapFactory.Options();
			op.inSampleSize = 1;
			op.inJustDecodeBounds = false;
			//op.inMutable = true;
			res = BitmapFactory.decodeFile(path, op);
			//rotate and scale.
			Matrix matrix = new Matrix();

			if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
				matrix.postRotate(90);
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
				matrix.postRotate(180);
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
				matrix.postRotate(270);
			}

			Bitmap temp = Bitmap.createBitmap(res, 0, 0, res.getWidth(), res.getHeight(), matrix, true);
			Log.d("com.arcsoft", "check target Image:" + temp.getWidth() + "X" + temp.getHeight());

			if (!temp.equals(res)) {
				res.recycle();
			}
			return temp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void Build_File(){
		String rootDir = Environment.getExternalStorageDirectory() + File.separator + "Video";
		mfile = new File(rootDir);
		if (!mfile.exists()){
			mfile.mkdirs();
		}
	}

	//读GPIO
	public String getGpioString(String path) {
		String defString = "99";// 默认值
		try {
			@SuppressWarnings("resource")
			BufferedReader reader = new BufferedReader(new FileReader(path));
			defString = reader.readLine();
		} catch (IOException e) {
			RootCommand("echo  263 > /sys/class/gpio/export");
			e.printStackTrace();
		}
		return defString;
	}


	//下面的是执行的方法
	private boolean RootCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			process.waitFor();
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				process.destroy();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}
