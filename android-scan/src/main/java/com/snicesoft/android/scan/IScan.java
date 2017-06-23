package com.snicesoft.android.scan;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.SurfaceHolder;

/**
 * Created by zhuzhe on 2017/6/23.
 */

public interface IScan extends SurfaceHolder.Callback {

    Rect getCropRect();

    void initCropRect();

    ScanHelper getScanHelper();

    void handleDecode(String obj, Bundle data);
}
