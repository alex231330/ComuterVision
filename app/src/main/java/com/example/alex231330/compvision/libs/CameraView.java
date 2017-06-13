package com.example.alex231330.compvision.libs;

import android.content.Context;
import android.graphics.*;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created with IntelliJ IDEA.
 * User: Egor Suvorov
 * Date: 01.08.13
 * Time: 10:18
 * To change this template use File | Settings | File Templates.
 */
public class CameraView extends ViewGroup {
    protected class CameraDrawer extends View {
        protected Bitmap toDraw;
        protected Matrix matrix;

        public CameraDrawer(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (toDraw != null && matrix != null)
                canvas.drawBitmap(toDraw, matrix, null);
        }

        public void setBitmapAndMatrix(Bitmap toDraw, Matrix matrix) {
            this.toDraw = toDraw;
            this.matrix = matrix;
        }
    }

    public static final int DATA_ROTATION_AUTO = -1;

    protected CameraSurface surface;
    protected CameraDrawer drawer;
    protected CameraListener listener;
    protected Bitmap toDraw;
    protected Canvas canvas;
    protected Matrix drawMatrix, invDrawMatrix;
    protected int preferredWidth, preferredHeight;
    protected CameraData cameraData;
    private int dataRotation;
    private long lastTime = Long.MIN_VALUE, lastProcessingTime = Long.MAX_VALUE;

    public CameraView(Context context) {
        super(context);
        surface = new CameraSurface(context);
        addView(surface);

        drawer = new CameraDrawer(context);
        addView(drawer);

        dataRotation = 0;

        surface.setCameraListener(new CameraListenerSimple() {
            @Override
            public void onCameraFrame(byte[] data, int width, int height, int cameraDisplayOrientation, Canvas _canvas) {
                lastProcessingTime = System.currentTimeMillis() - lastTime;
                lastTime = System.currentTimeMillis();
                if (listener != null) {
                    cameraData.setData(data);
                    listener.onCameraFrame(data, width, height, cameraDisplayOrientation, canvas);
                    listener.onCameraFrame(cameraData, cameraDisplayOrientation, canvas);
                    cameraData.setData(null);
                }
                drawer.setBitmapAndMatrix(toDraw, drawMatrix);
                drawer.invalidate();
            }

            @Override
            public void onSizeChange(int width, int height, int cameraDisplayOrientation) {
                if (toDraw != null) toDraw.recycle();
                int bitmapWidth, bitmapHeight;
                int currentDataRotation = dataRotation == DATA_ROTATION_AUTO ? cameraDisplayOrientation : dataRotation;
                if (currentDataRotation % 180 == 0) {
                    bitmapWidth = width;
                    bitmapHeight = height;
                } else {
                    bitmapWidth = height;
                    bitmapHeight = width;
                }

                toDraw = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                canvas = new Canvas(toDraw);

                int bitmapRotation = (cameraDisplayOrientation - currentDataRotation + 360) % 360;
                Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                drawMatrix = new Matrix();
                drawMatrix.postRotate(bitmapRotation);
                switch (bitmapRotation) {
                    case 0: break;
                    case 90: drawMatrix.postTranslate(bitmapHeight, 0); break;
                    case 180: drawMatrix.postTranslate(bitmapWidth, bitmapHeight); break;
                    case 270: drawMatrix.postTranslate(0, bitmapWidth); break;
                }
                if (cameraDisplayOrientation % 180 == 0) {
                    preferredWidth = width;
                    preferredHeight = height;
                } else {
                    preferredWidth = height;
                    preferredHeight = width;
                }
                drawMatrix.postScale(
                        (float) getWidth() / preferredWidth,
                        (float) getHeight() / preferredHeight
                );
                invDrawMatrix = new Matrix();
                if (!drawMatrix.invert(invDrawMatrix)) {
                    invDrawMatrix = null;
                }


                // Please note that CameraData's constructor automatically
                // adjusts getWidth()/getHeight() order based on currendDataRotation
                cameraData = new CameraData(width, height, currentDataRotation);
                if (listener != null)
                    listener.onSizeChange(bitmapWidth, bitmapHeight, cameraDisplayOrientation);

                // to avoid 'drawing recycled bitmap' error, if
                // drawer is re-drawed before new camera frame
                drawer.setBitmapAndMatrix(toDraw, drawMatrix);
            }
        });
    }

    public void setDataRotation(int newRotation) {
        if (newRotation != DATA_ROTATION_AUTO) {
            newRotation %= 360;
            if (newRotation < 0) newRotation += 360;
            if (newRotation % 90 != 0)
                throw new IllegalArgumentException("New rotation should be either DATA_ROTATION_AUTO or divisible by 90");
        }
        dataRotation = newRotation;
    }
    public int getDataRotation() {
        return dataRotation;
    }

    public PointF convertCoordsViewToCamera(float x, float y) {
        float[] data = { x, y };
        invDrawMatrix.mapPoints(data);
        return new PointF(data[0], data[1]);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (toDraw == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int modeW = MeasureSpec.getMode(widthMeasureSpec);
        int modeH = MeasureSpec.getMode(heightMeasureSpec);
        int parW = MeasureSpec.getSize(widthMeasureSpec);
        int parH = MeasureSpec.getSize(heightMeasureSpec);

        if (modeW == MeasureSpec.UNSPECIFIED && modeH == MeasureSpec.UNSPECIFIED) {
            setMeasuredDimension(preferredWidth, preferredHeight);
            return;
        }

        if (modeW == MeasureSpec.UNSPECIFIED) parW = Integer.MAX_VALUE;
        if (modeH == MeasureSpec.UNSPECIFIED) parH = Integer.MAX_VALUE;
        int newW = Math.min(parW, parH * preferredWidth / preferredHeight);
        int newH = Math.min(parH, parW * preferredHeight / preferredWidth);
        if (modeW == MeasureSpec.EXACTLY) newW = parW;
        if (modeH == MeasureSpec.EXACTLY) newH = parH;
        setMeasuredDimension(newW, newH);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++)
            getChildAt(i).layout(l, t, r, b);
    }

    public long getLastProcessingTime() {
        return lastProcessingTime;
    }

    public void setCameraListener(CameraListener listener) {
        this.listener = listener;
    }
}
