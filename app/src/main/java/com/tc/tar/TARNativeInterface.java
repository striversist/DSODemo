package com.tc.tar;

/**
 * Created by aarontang on 2017/3/30.
 */

public class TARNativeInterface {
    public static final String TAG = TARNativeInterface.class.getSimpleName();

    public static native void dsoInit(String calibPath);
    public static native void dsoRelease();
    public static native void dsoStart();
    public static native float[] dsoGetIntrinsics();
    public static native int[] dsoGetResolution();
    public static native float[] dsoGetCurrentPose();
    public static native Object[] dsoGetAllKeyFrames();
    public static native int dsoGetKeyFrameCount();
    public static native byte[] dsoGetCurrentImage();
}
