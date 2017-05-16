package com.tc.tar;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Process;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.ISurface;
import org.rajawali3d.view.SurfaceView;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DSOActivity extends AppCompatActivity implements DSORenderer.RenderListener {

    public static final String TAG = DSOActivity.class.getSimpleName();
    private static final boolean LOCAL_MODE = false;
    static {
        System.loadLibrary("DSO");
    }

    private String mFileDir;
    private RelativeLayout mLayout;
    private SurfaceView mRajawaliSurface;
    private Renderer mRenderer;
    private ImageView mImageView;
    private boolean mStopped = false;
    private VideoSource mVideoSource;
    private boolean mStarted = false;
    private TextView mTextView;
    private long mLastUpdateTime;
    private int mLastUpdateIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

        mRajawaliSurface = createSurfaceView();
        mRenderer = createRenderer();
        applyRenderer();

        mLayout = new RelativeLayout(this);
        FrameLayout.LayoutParams childParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mLayout.addView(mRajawaliSurface, childParams);

        mImageView = new ImageView(this);
        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(480, 320);
        imageParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        imageParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mLayout.addView(mImageView, imageParams);

        mTextView = new TextView(this);
        mTextView.setTextColor(Color.YELLOW);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(600, 100);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        textParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mLayout.addView(mTextView, textParams);

        mVideoSource = new VideoSource(this, Constants.IN_WIDTH, Constants.IN_HEIGHT);
        if (!LOCAL_MODE) {
            mVideoSource.start();
        }

        setContentView(mLayout);
    }

    private void init() {
        mFileDir = getExternalFilesDir(null).getAbsolutePath();
        Utils.copyAssets(this, mFileDir);
        TARNativeInterface.dsoInit(mFileDir + File.separator + "cameraCalibration.cfg");
    }

    protected SurfaceView createSurfaceView() {
        SurfaceView view = new SurfaceView(this);
        view.setFrameRate(60);
        view.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
        return view;
    }

    protected Renderer createRenderer() {
        DSORenderer renderer = new DSORenderer(this);
        renderer.setRenderListener(this);
        return renderer;
    }

    protected void applyRenderer() {
        mRajawaliSurface.setSurfaceRenderer(mRenderer);
    }

    public View getView() {
        return mLayout;
    }

    private void start() {
        DataThread thread = new DataThread("DSO_DataThread");
        thread.setOSPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        thread.start();
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
            mStarted = true;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // TODO: reset
            Toast.makeText(this, "Reset!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }

    @Override
    public void onRender() {
        if (mImageView == null)
            return;

        byte[] imgData;
        int imgDataWidth = 0;
        int imgDataHeight = 0;
        if (!mStarted) {
            byte[] frameData = mVideoSource.getFrame();     // YUV data
            if (frameData == null)
                return;

            imgData = new byte[Constants.IN_WIDTH * Constants.IN_HEIGHT * 4];
            for (int i = 0; i < imgData.length / 4; ++i) {
                imgData[i * 4] = frameData[i];
                imgData[i * 4 + 1] = frameData[i];
                imgData[i * 4 + 2] = frameData[i];
                imgData[i * 4 + 3] = (byte) 0xff;
            }
            imgDataWidth = Constants.IN_WIDTH;
            imgDataHeight = Constants.IN_HEIGHT;
        } else {
            imgData = TARNativeInterface.dsoGetCurrentImage();
            imgDataWidth = Constants.OUT_WIDTH;
            imgDataHeight = Constants.OUT_HEIGHT;
        }

        if (imgData == null)
            return;

        final Bitmap bm = Bitmap.createBitmap(imgDataWidth, imgDataHeight, Bitmap.Config.ARGB_8888);
        bm.copyPixelsFromBuffer(ByteBuffer.wrap(imgData));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(bm);
            }
        });
    }

    private void refreshFrameRate(final int frameIndex) {
        if (System.currentTimeMillis() - mLastUpdateTime < 1000) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int diffIndex = frameIndex - mLastUpdateIndex;
                float diffTime = (float)(System.currentTimeMillis() - mLastUpdateTime) / 1000;
                mTextView.setText("Frame Rate: " + (float)diffIndex / diffTime + " fps\ncurrent index: " + frameIndex);

                mLastUpdateTime = System.currentTimeMillis();
                mLastUpdateIndex = frameIndex;
            }
        });

    }

    private class DataThread extends Thread {
        private int mOSPriority = Process.THREAD_PRIORITY_DEFAULT;

        public DataThread(String threadName) {
            super(threadName);
        }

        public void setOSPriority(int priority) {
            mOSPriority = priority;
        }

        @Override
        public void run() {
            Process.setThreadPriority(mOSPriority);
            mLastUpdateTime = System.currentTimeMillis();

            int i = 0;
            String imgDir = Utils.getInnerSDCardPath() + "/LSD/images";

            if (LOCAL_MODE) {
                File directory = new File(imgDir);
                File[] files = directory.listFiles();
                Arrays.sort(files);
                for (final File file : files) {
                    if (mStopped)
                        return;
                    if (!file.getName().endsWith(".png"))
                        continue;
                    TARNativeInterface.dsoOnFrameByPath(imgDir + File.separator + file.getName());
                    refreshFrameRate(i++);
                }
            } else {
                while (!mStopped) {
                    byte[] frameData = mVideoSource.getFrame();     // YUV data
                    if (frameData != null) {
                        TARNativeInterface.dsoOnFrameByData(Constants.IN_WIDTH, Constants.IN_HEIGHT, frameData, 0);
                        refreshFrameRate(i++);
                    }
                }
            }
        }
    }
}
