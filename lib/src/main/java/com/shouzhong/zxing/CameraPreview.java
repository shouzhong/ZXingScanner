package com.shouzhong.zxing;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * 相机预览
 * <p>
 * 运行的主线为SurfaceHolder.Callback的三个回调方法
 */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    public static final String TAG = "CameraPreview";

    private CameraWrapper cameraWrapper;
    private Camera.PreviewCallback previewCallback;//当相机被释放时会被置为null
    private FocusAreaSetter focusAreaSetter;
    private SensorController sensorController;
    private Camera.AutoFocusCallback autoFocusCallback;

    private boolean previewing = false;//是否正在预览
    private boolean surfaceCreated = false;//surface是否已创建

    private int mPreviewWidth;
    private int mPreviewHeight;

    interface FocusAreaSetter {
        void setAutoFocusArea();
    }

    CameraPreview(Context context, int previewWidth, int previewHeight, CameraWrapper cameraWrapper, Camera.PreviewCallback previewCallback, FocusAreaSetter focusAreaSetter) {
        super(context);
        this.mPreviewWidth = previewWidth;
        this.mPreviewHeight = previewHeight;
        this.cameraWrapper = cameraWrapper;
        this.previewCallback = previewCallback;
        this.focusAreaSetter = focusAreaSetter;
        getHolder().addCallback(this);//surface生命周期的回调
        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * 重新测量宽高，使其预览不变形
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
//        setOptimalPreviewSize(width, height);
        int o = getResources().getConfiguration().orientation;
        float ratio = o == Configuration.ORIENTATION_PORTRAIT ? mPreviewHeight * 1f / mPreviewWidth : mPreviewWidth * 1f / mPreviewHeight;
        float r = width * 1f / height;
        if (ratio < r) height = (int) (width / ratio + 0.5f);
        else width = (int) (height * ratio + 0.5f);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!previewing) return super.onTouchEvent(event);
        try {
            cameraWrapper.camera.cancelAutoFocus();
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            final String currentFocusMode = parameters.getFocusMode();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            cameraWrapper.camera.setParameters(parameters);
            if (autoFocusCallback == null) autoFocusCallback = new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (previewing && cameraWrapper != null) {
                        cameraWrapper.camera.cancelAutoFocus();
                        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
                        parameters.setFocusMode(currentFocusMode);
                        cameraWrapper.camera.setParameters(parameters);
                    }
                }
            };
            if (cameraWrapper != null) cameraWrapper.camera.autoFocus(autoFocusCallback);
            return true;
        } catch (Exception e) {}
        return super.onTouchEvent(event);
    }

    //--------------------------------------------------------------------------------------------------

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
        stopCameraPreview();
    }

//--------------------------------------------------------------------------------------------------

    /**
     * 开始预览
     */
    void startCameraPreview() {
        if (cameraWrapper == null || previewing) return;
        try {
            //设置相机参数
            Camera.Parameters parameters = cameraWrapper.camera.getParameters();
            parameters.setPreviewSize(mPreviewWidth, mPreviewHeight);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            cameraWrapper.camera.setParameters(parameters);
            cameraWrapper.camera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
            cameraWrapper.camera.setPreviewDisplay(getHolder());//设置在当前surfaceView中进行相机预览
            cameraWrapper.camera.setDisplayOrientation(getDisplayOrientation());//设置相机预览图像的旋转角度
            cameraWrapper.camera.setOneShotPreviewCallback(previewCallback);//设置一次性的预览回调
            cameraWrapper.camera.startPreview();//开始预览
            previewing = true;
            if (!surfaceCreated || sensorController != null) return;
            focusAreaSetter.setAutoFocusArea();
            sensorController = new SensorController(getContext());
            sensorController.setDelay(500);
            sensorController.setCallback(new SensorController.Callback() {
                @Override
                public void onChanged() {
                    if (!previewing) return;
                    try {
                        cameraWrapper.camera.cancelAutoFocus();
                        Camera.Parameters parameters = cameraWrapper.camera.getParameters();
                        final String currentFocusMode = parameters.getFocusMode();
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                        cameraWrapper.camera.setParameters(parameters);
                        if (autoFocusCallback == null) autoFocusCallback = new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (previewing && cameraWrapper != null) {
                                    cameraWrapper.camera.cancelAutoFocus();
                                    Camera.Parameters parameters = cameraWrapper.camera.getParameters();
                                    parameters.setFocusMode(currentFocusMode);
                                    cameraWrapper.camera.setParameters(parameters);
                                }
                            }
                        };
                        if (cameraWrapper != null) cameraWrapper.camera.autoFocus(autoFocusCallback);
                    } catch (Exception e) {}
                }
            });
            sensorController.onStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览
     */
    void stopCameraPreview() {
        previewing = false;
        if (cameraWrapper != null) {
            try {
                if (sensorController != null) {
                    sensorController.onStop();
                    sensorController = null;
                }
                cameraWrapper.camera.cancelAutoFocus();
                cameraWrapper.camera.setOneShotPreviewCallback(null);
                cameraWrapper.camera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            cameraWrapper = null;
            previewCallback = null;
        }
    }

    /**
     * 要使相机图像的方向与手机中窗口的方向一致，相机图像需要顺时针旋转的角度
     * <p>
     * 此方法由google官方提供，详见Camera类中setDisplayOrientation的方法说明
     */
    int getDisplayOrientation() {
        if (cameraWrapper == null) return 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        if (cameraWrapper.cameraId == -1) {
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        } else {
            Camera.getCameraInfo(cameraWrapper.cameraId, info);
        }
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
}