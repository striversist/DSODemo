package com.tc.tar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.ISurface;
import org.rajawali3d.view.SurfaceView;

import java.io.File;
import java.nio.ByteBuffer;

public class DSOActivity extends AppCompatActivity implements DSORenderer.RenderListener {

    public static final String TAG = DSOActivity.class.getSimpleName();
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

        mVideoSource = new VideoSource(this, Constants.WIDTH, Constants.HEIGHT);
        mVideoSource.start();

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
        new Thread(new Runnable() {
            @Override
            public void run() {
                String imgDir = Utils.getInnerSDCardPath() + "/LSD/images";
                Log.d(TAG, "imgDir = " + imgDir);

                File directory = new File(imgDir);
                File[] files = directory.listFiles();
                for (final File file : files) {
                    if (mStopped)
                        return;
                    if (!file.getName().endsWith(".png"))
                        continue;

                    Log.d(TAG, "file: " + file.getName());
                    TARNativeInterface.dsoOnFrameByPath(imgDir + File.separator + file.getName());

//                    DSOActivity.this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            final Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
//                            assert (bitmap != null);
//                            mImageView.setImageBitmap(bitmap);
//                        }
//                    });
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
//            mStarted = true;
            start();
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
        byte[] frameData = mVideoSource.getFrame();     // YUV data
        if (frameData == null)
            return;

        imgData = new byte[Constants.WIDTH * Constants.HEIGHT * 4];
        for (int i = 0; i < imgData.length / 4; ++i) {
            imgData[i * 4] = frameData[i];
            imgData[i * 4 + 1] = frameData[i];
            imgData[i * 4 + 2] = frameData[i];
            imgData[i * 4 + 3] = (byte) 0xff;
        }

        final Bitmap bm = Bitmap.createBitmap(Constants.WIDTH, Constants.HEIGHT, Bitmap.Config.ARGB_8888);
        bm.copyPixelsFromBuffer(ByteBuffer.wrap(imgData));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(bm);
            }
        });

        if (mStarted) {
            TARNativeInterface.dsoOnFrameByData(Constants.WIDTH, Constants.HEIGHT, frameData, 0);
        }
    }
}
