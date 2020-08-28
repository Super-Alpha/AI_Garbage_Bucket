package com.arcsoft.sdk_demo.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import Decoder.BASE64Decoder;
import Decoder.BASE64Encoder;

public class Base64Image {

	public static String GetImageStr(String imapath)
    {
        InputStream in = null;
        byte[] data = null;

        try 
        {
            in = new FileInputStream(imapath);        
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }

        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(data);
    }
	

	public static boolean GenerateImage(String imgStr, String output)
    {
        if (imgStr == null)
            return false;
        BASE64Decoder decoder = new BASE64Decoder();
        try 
        {

            byte[] b = decoder.decodeBuffer(imgStr);
            for(int i=0;i<b.length;++i)
            {
                if(b[i]<0)
                {
                    b[i]+=256;
                }
            }
            OutputStream out = new FileOutputStream(output);    
            out.write(b);
            out.flush();
            out.close();
            return true;
        } 
        catch (Exception e) 
        {
            return false;
        }
    }

}
