package com.snicesoft.android.scan;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.client.android.Constants;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.android.decode.DecodeThread;

/**
 * Created by zhuzhe on 2017/6/23.
 */

public final class ScanHandler extends Handler {
    private final IScan scan;
    private final DecodeThread decodeThread;
    private CameraManager cameraManager;
    private State state;

    private enum State {
        PREVIEW,
        SUCCESS,
        DONE
    }

    public ScanHandler(IScan scan) {
        this.scan = scan;
        decodeThread = new DecodeThread((Activity) scan);
        decodeThread.start();
        state = State.SUCCESS;

        // Start ourselves capturing previews and decoding.
        this.cameraManager = scan.getScanHelper().getCameraManager();
        cameraManager.startPreview();
        restartPreviewAndDecode();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case Constants.ID_RESTART_PREVIEW:
                restartPreviewAndDecode();
                break;
            case Constants.ID_DECODE_SUCCESS:
                state = State.SUCCESS;
                scan.handleDecode((String) message.obj, message.getData());
                break;
            case Constants.ID_DECODE_FAILED:
                // We're decoding as fast as possible, so when one decode fails, start another.
                state = State.PREVIEW;
                cameraManager.requestPreviewFrame(decodeThread.getHandler(), Constants.ID_DECODE);
                break;
        }
    }

    public void quitSynchronously() {
        state = State.DONE;
        cameraManager.stopPreview();
        Message quit = Message.obtain(decodeThread.getHandler(), Constants.ID_QUIT);
        quit.sendToTarget();
        try {
            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            // continue
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(Constants.ID_DECODE_SUCCESS);
        removeMessages(Constants.ID_DECODE_FAILED);
    }

    private void restartPreviewAndDecode() {
        if (state == State.SUCCESS) {
            state = State.PREVIEW;
            cameraManager.requestPreviewFrame(decodeThread.getHandler(), Constants.ID_DECODE);
        }
    }
}
