package com.tc.tar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import java.io.File;

public class DSOActivity extends AppCompatActivity {

    static {
        System.loadLibrary("DSO");
    }

    private String mFileDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(R.layout.activity_dso);
    }

    private void init() {
        mFileDir = getExternalFilesDir(null).getAbsolutePath();
        Utils.copyAssets(this, mFileDir);
        TARNativeInterface.dsoInit(mFileDir + File.separator + "cameraCalibration.cfg");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            TARNativeInterface.dsoRelease();
            finish();
            System.exit(0);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
            TARNativeInterface.dsoStart();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            // TODO: reset
            Toast.makeText(this, "Reset!", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }
}
