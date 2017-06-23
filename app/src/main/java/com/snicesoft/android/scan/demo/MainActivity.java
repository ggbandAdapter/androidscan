package com.snicesoft.android.scan.demo;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.google.zxing.client.android.decode.DecodeThread;
import com.google.zxing.client.android.decode.DecodeUtils;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.model.TResult;
import com.nineoldandroids.view.ViewHelper;
import com.snicesoft.android.scan.IScan;
import com.snicesoft.android.scan.ScanHelper;

import java.util.regex.Pattern;

public class MainActivity extends TakePhotoActivity implements IScan {

    ScanHelper scanHelper;
    public static final int IMAGE_PICKER_REQUEST_CODE = 100;

    SurfaceView capturePreview;
    ImageView captureErrorMask;
    ImageView captureScanMask;
    FrameLayout captureCropView;
    Button capturePictureBtn;
    Button captureLightBtn;
    RadioGroup captureModeGroup;
    RelativeLayout captureContainer;
    private int mQrcodeCropWidth = 0;
    private int mQrcodeCropHeight = 0;
    private int mBarcodeCropWidth = 0;
    private int mBarcodeCropHeight = 0;

    private ObjectAnimator mScanMaskObjectAnimator = null;
    private Rect cropRect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        scanHelper = new ScanHelper(this, capturePreview) {

            @Override
            public void onCameraPreviewSuccess() {
                initCropRect();
                captureErrorMask.setVisibility(View.GONE);

                ViewHelper.setPivotX(captureScanMask, 0.0f);
                ViewHelper.setPivotY(captureScanMask, 0.0f);

                mScanMaskObjectAnimator = ObjectAnimator.ofFloat(captureScanMask, "scaleY", 0.0f, 1.0f);
                mScanMaskObjectAnimator.setDuration(2000);
                mScanMaskObjectAnimator.setInterpolator(new DecelerateInterpolator());
                mScanMaskObjectAnimator.setRepeatCount(-1);
                mScanMaskObjectAnimator.setRepeatMode(ObjectAnimator.RESTART);
                mScanMaskObjectAnimator.start();
            }

            @Override
            public void displayFrameworkBugMessageAndExit() {
                captureErrorMask.setVisibility(View.VISIBLE);
                final MaterialDialog.Builder builder = new MaterialDialog.Builder(MainActivity.this);
                builder.cancelable(true);
                builder.title(R.string.app_name);
                builder.content(R.string.tips_open_camera_error);
                builder.positiveText(R.string.btn_ok);
                builder.callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        finish();
                    }
                });
                builder.show();
            }
        };
        initCropViewAnimator();
        capturePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTakePhoto().onPickFromGallery();
//                        readyGoForResult(CommonImagePickerListActivity.class, IMAGE_PICKER_REQUEST_CODE);
            }
        });

        captureLightBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanHelper.toggleLightOn();
                checkLightStatus();
            }
        });

        captureModeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.capture_mode_barcode) {
                    PropertyValuesHolder qr2barWidthVH = PropertyValuesHolder.ofFloat("width",
                            1.0f, (float) mBarcodeCropWidth / mQrcodeCropWidth);
                    PropertyValuesHolder qr2barHeightVH = PropertyValuesHolder.ofFloat("height",
                            1.0f, (float) mBarcodeCropHeight / mQrcodeCropHeight);
                    ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(qr2barWidthVH, qr2barHeightVH);
                    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Float fractionW = (Float) animation.getAnimatedValue("width");
                            Float fractionH = (Float) animation.getAnimatedValue("height");

                            RelativeLayout.LayoutParams parentLayoutParams = (RelativeLayout.LayoutParams) captureCropView.getLayoutParams();
                            parentLayoutParams.width = (int) (mQrcodeCropWidth * fractionW);
                            parentLayoutParams.height = (int) (mQrcodeCropHeight * fractionH);
                            captureCropView.setLayoutParams(parentLayoutParams);
                        }
                    });
                    valueAnimator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            initCropRect();
                            scanHelper.setDataMode(DecodeUtils.DECODE_DATA_MODE_BARCODE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    valueAnimator.start();


                } else if (checkedId == R.id.capture_mode_qrcode) {
                    PropertyValuesHolder bar2qrWidthVH = PropertyValuesHolder.ofFloat("width",
                            1.0f, (float) mQrcodeCropWidth / mBarcodeCropWidth);
                    PropertyValuesHolder bar2qrHeightVH = PropertyValuesHolder.ofFloat("height",
                            1.0f, (float) mQrcodeCropHeight / mBarcodeCropHeight);
                    ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(bar2qrWidthVH, bar2qrHeightVH);
                    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Float fractionW = (Float) animation.getAnimatedValue("width");
                            Float fractionH = (Float) animation.getAnimatedValue("height");

                            RelativeLayout.LayoutParams parentLayoutParams = (RelativeLayout.LayoutParams) captureCropView.getLayoutParams();
                            parentLayoutParams.width = (int) (mBarcodeCropWidth * fractionW);
                            parentLayoutParams.height = (int) (mBarcodeCropHeight * fractionH);
                            captureCropView.setLayoutParams(parentLayoutParams);
                        }
                    });
                    valueAnimator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            initCropRect();
                            scanHelper.setDataMode(DecodeUtils.DECODE_DATA_MODE_QRCODE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                    valueAnimator.start();
                }
            }
        });
    }

    private void checkLightStatus() {
        if (scanHelper.isLightOn()) {
            captureLightBtn.setSelected(true);
        } else {
            captureLightBtn.setSelected(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanHelper.onResume();
        checkLightStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanHelper.onPause();
        if (null != mScanMaskObjectAnimator && mScanMaskObjectAnimator.isStarted()) {
            mScanMaskObjectAnimator.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanHelper.onDestroy();
    }

    public void setCropRect(Rect cropRect) {
        this.cropRect = cropRect;
    }

    private void initViews() {
        capturePreview = (SurfaceView) findViewById(R.id.capture_preview);
        captureErrorMask = (ImageView) findViewById(R.id.capture_error_mask);
        captureScanMask = (ImageView) findViewById(R.id.capture_scan_mask);
        captureCropView = (FrameLayout) findViewById(R.id.capture_crop_view);
        capturePictureBtn = (Button) findViewById(R.id.capture_picture_btn);
        captureLightBtn = (Button) findViewById(R.id.capture_light_btn);
        captureModeGroup = (RadioGroup) findViewById(R.id.capture_mode_group);
        captureContainer = (RelativeLayout) findViewById(R.id.capture_container);
    }

    private void initCropViewAnimator() {
        mQrcodeCropWidth = getResources().getDimensionPixelSize(R.dimen.qrcode_crop_width);
        mQrcodeCropHeight = getResources().getDimensionPixelSize(R.dimen.qrcode_crop_height);

        mBarcodeCropWidth = getResources().getDimensionPixelSize(R.dimen.barcode_crop_width);
        mBarcodeCropHeight = getResources().getDimensionPixelSize(R.dimen.barcode_crop_height);
    }

    @Override
    public Rect getCropRect() {
        return cropRect;
    }

    @Override
    public void initCropRect() {
        int cameraWidth = scanHelper.getCameraManager().getCameraResolution().y;
        int cameraHeight = scanHelper.getCameraManager().getCameraResolution().x;

        int containerWidth = captureContainer.getWidth();
        int containerHeight = captureContainer.getHeight();

        int cropLeft = captureCropView.getLeft();
        int cropTop = captureCropView.getTop();
        int cropWidth = captureCropView.getWidth();
        int cropHeight = captureCropView.getHeight();

        int x = cropLeft * cameraWidth / containerWidth;
        int y = cropTop * cameraHeight / containerHeight;
        int width = cropWidth * cameraWidth / containerWidth;
        int height = cropHeight * cameraHeight / containerHeight;

        setCropRect(new Rect(x, y, width + x, height + y));
    }

    @Override
    public ScanHelper getScanHelper() {
        return scanHelper;
    }

    @Override
    public void handleDecode(String result, Bundle bundle) {
        scanHelper.handleDecode();
        if (!TextUtils.isEmpty(result) && isUrl(result)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(result));
            startActivity(intent);
        } else {
            bundle.putString(ResultActivity.BUNDLE_KEY_SCAN_RESULT, result);
            readyGo(ResultActivity.class, bundle);
        }
    }

    /**
     * is url
     *
     * @param url
     * @return
     */
    public static boolean isUrl(String url) {
        Pattern pattern = Pattern.compile("^([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+$");
        return pattern.matcher(url).matches();
    }

    /**
     * startActivity with bundle
     *
     * @param clazz
     * @param bundle
     */
    protected void readyGo(Class<?> clazz, Bundle bundle) {
        Intent intent = new Intent(this, clazz);
        if (null != bundle) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        scanHelper.surfaceCreated();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        scanHelper.surfaceChanged(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        scanHelper.surfaceDestoryed();
    }

    @Override
    public void takeSuccess(TResult result) {
        Glide.with(this).load(result.getImage().getOriginalPath()).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap loadedImage, GlideAnimation<? super Bitmap> glideAnimation) {
                String resultZxing = new DecodeUtils(DecodeUtils.DECODE_DATA_MODE_ALL)
                        .decodeWithZxing(loadedImage);
                String resultZbar = new DecodeUtils(DecodeUtils.DECODE_DATA_MODE_ALL)
                        .decodeWithZbar(loadedImage);

                if (!TextUtils.isEmpty(resultZbar)) {
                    Bundle extras = new Bundle();
                    extras.putInt(DecodeThread.DECODE_MODE, DecodeUtils.DECODE_MODE_ZBAR);

                    handleDecode(resultZbar, extras);
                } else if (!TextUtils.isEmpty(resultZxing)) {
                    Bundle extras = new Bundle();
                    extras.putInt(DecodeThread.DECODE_MODE, DecodeUtils.DECODE_MODE_ZXING);

                    handleDecode(resultZxing, extras);
                } else {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.tips_decode_null), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void takeFail(TResult result, String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void takeCancel() {
        Toast.makeText(this, "cancel", Toast.LENGTH_SHORT).show();
    }
}
