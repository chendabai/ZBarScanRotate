package com.dabai.zbarscanrotate.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dabai.zbarscanrotate.R;
import com.dabai.zbarscanrotate.scan.CameraManager;
import com.dabai.zbarscanrotate.scan.CameraPreview;
import com.dabai.zbarscanrotate.scan.ScreenOrientationChange;
import com.dabai.zbarscanrotate.scan.ScreenSwitchUtils;
import com.dtr.zbar.build.ZBarDecoder;

import java.io.IOException;
import java.lang.reflect.Field;

@SuppressLint("NewApi")
public class CaptureActivity extends Activity implements ScreenOrientationChange {
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private CameraManager mCameraManager;
    private TextView tv_back;
    private FrameLayout scanPreview;
    private RelativeLayout scanContainer;
    private RelativeLayout scanCropView;
    private ImageView scanLine;
    private TextView tv_status_view;
    private Rect mCropRect = null;
    private boolean barcodeScanned = false;
    private boolean previewing = true;

    private ScreenSwitchUtils instance;
    //是否是竖屏
    private boolean isPortrait = true;
    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Size size = camera.getParameters().getPreviewSize();
            // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
            byte[] rotatedData = new byte[data.length];
            for (int y = 0; y < size.height; y++) {
                for (int x = 0; x < size.width; x++)
                    rotatedData[x * size.height + size.height - y - 1] = data[x + y * size.width];
            }

            // 宽高也要调整
            int tmp = size.width;
            size.width = size.height;
            size.height = tmp;
            initCrop();
            ZBarDecoder zBarDecoder = new ZBarDecoder();

            //横竖屏矩形区域判断
            int left = 0;
            if (isPortrait) {
                left = mCropRect.left;
            } else {
                left = mCropRect.left - mCropRect.width();
            }

            String result = zBarDecoder.decodeCrop(rotatedData, size.width, size.height, left, mCropRect.top, mCropRect.width(), mCropRect.height());
            Log.d("code", "mCropRect.left:" + mCropRect.left + "mCropRect.left:" + mCropRect.top + "mCropRect.width:" + mCropRect.width() + "mCropRect.height" + mCropRect.height());
            if (!TextUtils.isEmpty(result)) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                Log.d(TAG, "扫描成功: " + result);
                Intent intent = getIntent();
                intent.putExtra("BarcodeFormat", result);
                intent.putExtra("DisplayContents", result);
                setResult(MainActivity.CODE_SCAN, intent);
                finish();
                barcodeScanned = true;
            }
        }
    };
    private TranslateAnimation animation;
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };
    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        findViewById();
        addEvents();
        initViews();
        instance = ScreenSwitchUtils.init(CaptureActivity.this);
    }


    private void findViewById() {
        scanPreview = (FrameLayout) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);
        tv_back = (TextView) findViewById(R.id.common_title_TV_left);
        tv_status_view = (TextView) findViewById(R.id.status_view);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void addEvents() {
        tv_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = getIntent();
//				intent.putExtra("BarcodeFormat", result);
//				intent.putExtra("DisplayContents", result);
                setResult(0, intent);
                finish();
            }
        });
    }

    private void initCamera() {
        mCamera.setPreviewCallback(previewCb);
        mCamera.startPreview();
        previewing = true;
        mCamera.autoFocus(autoFocusCB);
    }

    private void initViews() {
        autoFocusHandler = new Handler();
        mCameraManager = new CameraManager(this);
        try {
            mCameraManager.openDriver();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            Toast.makeText(CaptureActivity.this, "相机被其他应用占用或未允许相机访问权限，请进入设置界面给予相机访问权限！", Toast.LENGTH_SHORT).show();
            finish();
        }

        mCamera = mCameraManager.getCamera();
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        scanPreview.addView(mPreview);
        //初始化相机动画
        initAnimation();
    }

    private void initAnimation() {
        if (null != scanLine) {
            scanLine.clearAnimation();
        }
        if (null != animation) {
            animation.reset();
            animation.cancel();
            animation = null;
        }
        animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT,
                0.0f, Animation.RELATIVE_TO_PARENT,
                0.0f, Animation.RELATIVE_TO_PARENT,
                0.85f);
        animation.setDuration(2000);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.REVERSE);
        scanLine.startAnimation(animation);
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        instance.destroy();
        Log.e(TAG, "我被销毁了");
        //销毁时释放全部资源
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if (null != scanLine) {
            scanLine.clearAnimation();
            scanLine = null;
            scanContainer = null;
            scanCropView = null;
        }
        super.onDestroy();

    }

    @Override
    protected void onResume() {
        previewing = true;
        super.onResume();

    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
        }
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = mCameraManager.getCameraResolution().y;
        int cameraHeight = mCameraManager.getCameraResolution().x;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();

        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = cropLeft * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = cropTop * cameraHeight / containerHeight;

        /** 计算最终截取的矩形的宽度 */
        int width = cropWidth * cameraWidth / containerWidth;
        /** 计算最终截取的矩形的高度 */
        int height = cropHeight * cameraHeight / containerHeight;

        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    @Override
    protected void onStart() {
        super.onStart();
        instance.start(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        instance.stop();
    }


    private int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void change(int orientation) {


        Log.e(TAG, "address---scanContainer" + scanContainer.toString() + "----scanCropView" + scanCropView.toString());
        switch (orientation) {
            case ScreenOrientationChange.TOP:
                if (null != scanCropView) {
                    Log.e(TAG, "上");
                    isPortrait = true;
                    scanCropView.setRotation(0);
                    tv_status_view.setVisibility(View.VISIBLE);

                }
                break;
            case ScreenOrientationChange.BOTTOM:
                if (null != scanCropView) {
                    Log.e(TAG, "下");
                    isPortrait = true;
                    scanCropView.setRotation(0);
                    tv_status_view.setVisibility(View.VISIBLE);

                }
                break;
            case ScreenOrientationChange.LEFT:
                if (null != scanCropView) {
                    Log.e(TAG, "左");
                    isPortrait = false;
                    scanCropView.setRotation(90);
                    tv_status_view.setVisibility(View.GONE);

                }
                break;
            case ScreenOrientationChange.RIGHT:
                if (null != scanCropView) {
                    Log.e(TAG, "右");
                    isPortrait = false;
                    scanCropView.setRotation(90);
                    tv_status_view.setVisibility(View.GONE);

                }
                break;
            default:
                break;
        }
    }


}
