package com.tc.tar;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by aarontang on 2017/4/18.
 */

public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static void dumpFrame(byte[] data, int width, int height, int frameIndex) {
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/LSD/dump/" +
                    String.format("%05d", frameIndex) + ".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyAssets(Context context, String dir) {
        AssetManager assetManager = context.getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e(TAG, "copyAssets: Failed to get asset file list.", e);
        }
        for(String filename : files) {
            if(!filename.endsWith(".cfg"))//hack to skip non cfg files
                continue;
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open(filename);
                File outFile = new File(dir, filename);
                if(outFile.exists())
                {
                    Log.d(TAG, "copyAssets: File exists: " + filename);
                }
                else
                {
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    Log.d(TAG, "copyAssets: File copied: " + filename);
                }
            } catch(IOException e) {
                Log.e(TAG, "copyAssets: Failed to copy asset file: " + filename, e);
            }
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            }
        }
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public static String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static byte [] getNV12(int inputWidth, int inputHeight, Bitmap scaled) {
        // Reference (Variation) : https://gist.github.com/wobbals/5725412

        int [] argb = new int[inputWidth * inputHeight];

        //Log.i(TAG, "scaled : " + scaled);
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }

    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                R = (argb[index] & 0xff000000) >>> 24;
                G = (argb[index] & 0xff0000) >> 16;
                B = (argb[index] & 0xff00) >> 8;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                V = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128; // Previously V

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }
}
