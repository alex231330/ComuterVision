package com.example.alex231330.compvision.libs;

import android.graphics.Canvas;

/**
 * Created with IntelliJ IDEA.
 * User: Egor Suvorov
 * Date: 01.08.13
 * Time: 10:26
 * To change this template use File | Settings | File Templates.
 */
public interface CameraListenerSimple {
    public void onCameraFrame(byte[] data, int width, int height, int cameraDisplayOrientation, Canvas canvas);

    public void onSizeChange(int width, int height, int cameraDisplayOrientation);
}
