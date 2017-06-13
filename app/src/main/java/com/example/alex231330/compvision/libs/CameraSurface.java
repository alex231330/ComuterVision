package com.example.alex231330.compvision.libs;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.*;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Egor Suvorov
 * Date: 29.07.13
 * Time: 20:54
 * To change this template use File | Settings | File Templates.
 */
public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    protected Camera camera;
    protected CameraListenerSimple listener = null;
    protected int cameraId;

    private static int findCameraByFacing(int facing) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == facing)
                return i;
        }
        return -1;
    }

    public CameraSurface(Context context) {
        super(context);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCameraListener(CameraListenerSimple listener) {
        this.listener = listener;
    }

    protected int estimateBufferSize(Camera.Parameters parameters) {
        int format = parameters.getPreviewFormat();
        Camera.Size size = parameters.getPreviewSize();

        if (format != ImageFormat.NV21) {
            throw new RuntimeException("Invalid image format for camera preview");
        }
        int bits = ImageFormat.getBitsPerPixel(format);
        return (size.width * size.height * bits + 7) / 8;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        cameraId = findCameraByFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
        camera = Camera.open(cameraId);

        Camera.Parameters parameters = camera.getParameters();

        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            throw new RuntimeException("Cannot setPreviewDisplay", e);
        }
    }

    int[] argbBuffer;
    Camera.Size size;
    int cameraDisplayOrientation;

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        camera.stopPreview();

        Camera.Parameters parameters = camera.getParameters();
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        size = parameters.getPreviewSize();
        argbBuffer = new int[size.width * size.height];

        Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation%28int%29
        int displayRotation = 0;
        switch (display.getRotation()) {
            case Surface.ROTATION_0: displayRotation = 0; break;
            case Surface.ROTATION_90: displayRotation = 90; break;
            case Surface.ROTATION_180: displayRotation = 180; break;
            case Surface.ROTATION_270: displayRotation = 270; break;
        }
        int cameraDisplayOrientation;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraDisplayOrientation = (info.orientation + displayRotation) % 360;
            cameraDisplayOrientation = (360 - cameraDisplayOrientation) % 360;
        } else {
            cameraDisplayOrientation = (info.orientation - displayRotation + 360) % 360;
        }
        camera.setDisplayOrientation(cameraDisplayOrientation);
        this.cameraDisplayOrientation = cameraDisplayOrientation;
        requestLayout();

        if (listener != null)
            listener.onSizeChange(size.width, size.height, cameraDisplayOrientation);

        camera.setPreviewCallbackWithBuffer(this);
        camera.addCallbackBuffer(new byte[estimateBufferSize(parameters)]);

        camera.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (listener != null)
            listener.onCameraFrame(bytes, size.width, size.height, cameraDisplayOrientation, null);
        camera.addCallbackBuffer(bytes);
    }

    public static int convertYCrCbToRgb(int y, int cr, int cb) {
        cr -= 128;
        cb -= 128;

        int r, g, b;

        r = (int)(y + 1.402 * cr);
        g = (int)(y - 0.34414 * cb - 0.71414 * cr);
        b = (int)(y + 1.772 * cb);
        r = r > 255 ? 255 : r < 0 ? 0 : r;
        g = g > 255 ? 255 : g < 0 ? 0 : g;
        b = b > 255 ? 255 : b < 0 ? 0 : b;

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public static int getColor(byte[] bytes, int x, int y, int width, int height) {
        int Y = bytes[y * width + x] & 0xFF;
        int offCrCb = height * width + (y >> 1) * width + (x - (x & 1));
        int Cr = bytes[offCrCb] & 0xFF;
        int Cb = bytes[offCrCb + 1] & 0xFF;
        return convertYCrCbToRgb(Y, Cr, Cb);
    }
}
