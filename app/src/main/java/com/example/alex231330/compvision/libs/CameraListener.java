package com.example.alex231330.compvision.libs;

import android.graphics.Canvas;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 09.04.14
 * Time: 20:53
 * To change this template use File | Settings | File Templates.
 */
public interface CameraListener extends CameraListenerSimple {
    public void onCameraFrame(CameraData data, int cameraDisplayOrientation, Canvas canvas);
}
