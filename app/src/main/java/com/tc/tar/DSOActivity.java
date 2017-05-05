package com.tc.tar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.SurfaceView;

import java.io.File;

public class DSOActivity extends AppCompatActivity {

    public static final String TAG = DSOActivity.class.getSimpleName();
    static {
        System.loadLibrary("DSO");
    }

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private String mFileDir;
    private RelativeLayout mLayout;
    private SurfaceView mRajawaliSurface;
    private Renderer mRenderer;
    private ImageView mImageView;
    private boolean mStopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        mLayout = new RelativeLayout(this);
//        FrameLayout.LayoutParams childParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams
//                .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        mLayout.addView(mRajawaliSurface, childParams);

        mImageView = new ImageView(this);
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(480, 320);
        imageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        imageParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mLayout.addView(mImageView, imageParams);

        setContentView(mLayout);
    }

    private void init() {
        mFileDir = getExternalFilesDir(null).getAbsolutePath();
        Utils.copyAssets(this, mFileDir);
        TARNativeInterface.dsoInit(mFileDir + File.separator + "cameraCalibration.cfg");
    }

    private void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String imgDir = Utils.getInnerSDCardPath() + "/LSD/images";
                Log.d(TAG, "imgDir = " + imgDir);

                File directory = new File(imgDir);
                File[] files = directory.listFiles();
                for (File file : files) {
                    if (mStopped)
                        return;
                    if (!file.getName().endsWith(".png"))
                        continue;
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    assert (bitmap != null);
                    byte [] yuvData = Utils.getNV12(WIDTH, HEIGHT, bitmap);
                    TARNativeInterface.dsoOnFrame(WIDTH, HEIGHT, yuvData, 0);
                }
            }
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mStopped = true;
            TARNativeInterface.dsoRelease();
            finish();
            System.exit(0);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
            start();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // TODO: reset
            Toast.makeText(this, "Reset!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }
}
