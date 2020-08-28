package com.arcsoft.sdk_demo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.java.ExtInputStream;
import com.guo.android_extend.java.ExtOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
/**
 * 将人脸特征缓存在SD卡中
 * Created by gqj3375 on 2017/7/11.
 */
public class FaceDB {
	private final String TAG = this.getClass().toString();
	public static String appid = "2MATpvWAwmPQrtk9pHsmw9Vq45CtJMvFfLsgAWPJAeqD";
	public static String ft_key = "9YKPwuFuLdTd7vNm9MJ7XtnGaaaqJnGJEVAZocQaEvLY";
	public static String fd_key = "9YKPwuFuLdTd7vNm9MJ7XtnPjyqzngBoN9bmjNeXoGD4";
	public static String fr_key = "9YKPwuFuLdTd7vNm9MJ7XtntPathjQdGZazFjyqBjTGL";
	public static String age_key = "9YKPwuFuLdTd7vNm9MJ7Xto8iPR3xKjtqh1pbugHeBbC";
	public static String gender_key = "9YKPwuFuLdTd7vNm9MJ7XtoFsngCcJdTbwoGHjSTm69s";

	String mDBPath;
	List<FaceRegist> mRegister;
	AFR_FSDKEngine mFREngine;
	AFR_FSDKVersion mFRVersion;
	boolean mUpgrade;

	class FaceRegist {
		String mName;
		Map<String, AFR_FSDKFace> mFaceList;
		public FaceRegist(String name) {
			mName = name;
			mFaceList = new LinkedHashMap<>();
		}
	}

	public FaceDB(String path){
		mDBPath = path;
		mRegister = new ArrayList<>();
		mFRVersion = new AFR_FSDKVersion();
		mUpgrade = false;
		File file = new File(path+"/face.txt");
		mFREngine = new AFR_FSDKEngine();
		AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
		if (error.getCode() != AFR_FSDKError.MOK) {
			Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
		} else {
			mFREngine.AFR_FSDK_GetVersion(mFRVersion);
			Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
		}
		if (file.exists()){
			System.out.print("文件已经存在");
		}else{
			try{
				file.createNewFile();
				Log.d("文件路径为：",path+"/face.txt");
			}catch (IOException e){
				e.printStackTrace();
			}
		}
	}

	public void destroy() {
		if (mFREngine != null) {
			mFREngine.AFR_FSDK_UninitialEngine();
		}
	}

	private boolean saveInfo() {
		try {
			FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt");
			ExtOutputStream bos = new ExtOutputStream(fs);
			bos.writeString(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel());
			bos.close();
			fs.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean loadInfo() {
		if (!mRegister.isEmpty()) {
			return false;
		}
		try {
			FileInputStream fs = new FileInputStream(mDBPath + "/face.txt");
			ExtInputStream bos = new ExtInputStream(fs);
			//load version
			String version_saved = bos.readString();
			if (version_saved.equals(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel())){
				mUpgrade = true;
			}
			//load all regist name.
			if (version_saved != null) {
				for (String name = bos.readString(); name != null; name = bos.readString()){
					if (new File(mDBPath + "/" + name + ".data").exists()) {
						mRegister.add(new FaceRegist(new String(name)));
					}
				}
			}
			bos.close();
			fs.close();
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
    //从文件中读取人脸
	public boolean loadFaces(){
		if (loadInfo()) {
			try {
				for (FaceRegist face : mRegister) {
					Log.d(TAG, "load name:" + face.mName + "'s face feature data.");
					FileInputStream fs = new FileInputStream(mDBPath + "/" + face.mName + ".data");
					ExtInputStream bos = new ExtInputStream(fs);
					AFR_FSDKFace afr = null;
					do {
						if (afr != null) {
							if (mUpgrade) {
								//upgrade data.
							}
							String keyFile = bos.readString();
							face.mFaceList.put(keyFile, afr);
						}
						afr = new AFR_FSDKFace();
					} while (bos.readBytes(afr.getFeatureData()));
					bos.close();
					fs.close();
					Log.d(TAG, "load name: size = " + face.mFaceList.size());
				}
				return true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public void addFace(String name, AFR_FSDKFace face, Bitmap faceicon) {
		/**
		 * name:注册用户的姓名
		 * face：注册用户的脸部特征
		 * faceicon：注册用户的人脸图片
		 */
		try {
			// save face
			String keyPath = mDBPath + "/" + System.nanoTime() + ".jpg";//返回的是纳秒
			File keyFile = new File(keyPath);
			OutputStream stream = new FileOutputStream(keyFile);
			if (faceicon.compress(Bitmap.CompressFormat.JPEG, 80, stream)){//压缩保存
				Log.d(TAG, "saved face bitmap to jpg!");
			}
			stream.close();

			//check if already registered.
			boolean add = true;
			for (FaceRegist frface : mRegister) {
				if (frface.mName.equals(name)) {
					frface.mFaceList.put(keyPath, face);//对于已经注册的用户而言，可以用来更新人脸特征
					add = false;
					break;
				}
			}
			if (add) { // not registered.对于没有注册的用户，注册姓名、人脸图片、脸部特征
				FaceRegist frface = new FaceRegist(name);
				frface.mFaceList.put(keyPath, face);//keyPath代表人脸图片的路径
				mRegister.add(frface);
			}
			if (saveInfo()) {
				//update all names
				FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
				ExtOutputStream bos = new ExtOutputStream(fs);
				for (FaceRegist frface : mRegister) {
					bos.writeString(frface.mName);
				}
				bos.close();
				fs.close();

				//save new feature 将脸部信息进行保存，保存在name.data之中
				fs = new FileOutputStream(mDBPath + "/" + name + ".data", true);
				bos = new ExtOutputStream(fs);
				bos.writeBytes(face.getFeatureData());
				bos.writeString(keyPath);
				bos.close();
				fs.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public boolean delete(String name) {
		try {
			//check if already registered.
			boolean find = false;
			for (FaceRegist frface : mRegister) {
				if (frface.mName.equals(name)) {
					File delfile = new File(mDBPath + "/" + name + ".data");
					if (delfile.exists()) {
						delfile.delete();
					}
					mRegister.remove(frface);
					find = true;
					break;
				}
			}
			if (find) {
				if (saveInfo()) {
					//update all names
					FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
					ExtOutputStream bos = new ExtOutputStream(fs);
					for (FaceRegist frface : mRegister) {
						bos.writeString(frface.mName);
					}
					bos.close();
					fs.close();
				}
			}
			return find;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	public boolean upgrade() {
		return false;
	}
}
