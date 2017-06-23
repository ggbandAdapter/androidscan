package com.snicesoft.android.scan;

import android.app.Activity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.client.android.BeepManager;
import com.google.zxing.client.android.InactivityTimer;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.decode.DecodeUtils;

import java.io.IOException;

/**
 * Created by zhuzhe on 2017/6/23.
 */

public abstract class ScanHelper {

    private final static String TAG_LOG = "ScanHelper";

    private Activity activity;

    private CameraManager cameraManager;
    private ScanHandler handler;
    private InactivityTimer mInactivityTimer;
    private BeepManager mBeepManager;

    SurfaceView capturePreview;

    private boolean hasSurface;
    private boolean isLightOn;
    private int dataMode = DecodeUtils.DECODE_DATA_MODE_QRCODE;

    public ScanHelper(Activity activity, SurfaceView surfaceView) {
        this.activity = activity;
        this.capturePreview = surfaceView;
        this.hasSurface = false;
        this.mInactivityTimer = new InactivityTimer(activity);
        this.mBeepManager = new BeepManager(activity);
    }

    /**
     * 开关灯光
     */
    public void toggleLightOn() {
        if (isLightOn) {
            getCameraManager().setTorch(false);
        } else {
            getCameraManager().setTorch(true);
        }
        isLightOn = !isLightOn;
    }

    public boolean isLightOn() {
        return isLightOn;
    }

    public void onResume() {
        cameraManager = new CameraManager(activity.getApplication());

        handler = null;

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            surfaceChanged(capturePreview.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            capturePreview.getHolder().addCallback((SurfaceHolder.Callback) activity);
        }

        mInactivityTimer.onResume();
    }

    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }

        mBeepManager.close();
        mInactivityTimer.onPause();
        cameraManager.closeDriver();

        if (!hasSurface) {
            capturePreview.getHolder().removeCallback((SurfaceHolder.Callback) activity);
        }
    }

    public void onDestroy() {
        mInactivityTimer.shutdown();
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public ScanHandler getHandler() {
        return handler;
    }

    public int getDataMode() {
        return dataMode;
    }

    public void setDataMode(int dataMode) {
        this.dataMode = dataMode;
    }

    public void surfaceCreated() {
        if (!hasSurface) {
            hasSurface = true;
        }
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG_LOG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new ScanHandler((IScan) activity);
            }

            onCameraPreviewSuccess();
        } catch (IOException ioe) {
            Log.w(TAG_LOG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG_LOG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }

    }

    public void surfaceDestoryed() {
        hasSurface = false;
    }

    public void handleDecode() {
        mInactivityTimer.onActivity();
        mBeepManager.playBeepSoundAndVibrate();
    }

    public abstract void onCameraPreviewSuccess();

    public abstract void displayFrameworkBugMessageAndExit();
}
