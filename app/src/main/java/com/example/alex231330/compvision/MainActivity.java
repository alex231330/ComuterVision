package com.example.alex231330.compvision;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.alex231330.compvision.libs.CameraData;
import com.example.alex231330.compvision.libs.CameraListener;
import com.example.alex231330.compvision.libs.CameraView;

import java.util.Vector;

public class MainActivity extends Activity {

    private final String TAG = "MyApp";
    private final String CTAG = "Count";
    private Button calibrate;
    private SeekBar fseek;
    private boolean status = false;
    private int RED = 0;
    private int GREEN = 0;
    private int BLUE = 0;
    private int rectSize = 100;
    private int fault = 10;
    private TextView dater;
    private Bitmap map = null;
    private Vector figure = new Vector();
    private Canvas pcan;
    private int count = 0;
    private int save = 0;
    private int y0 = 0;
    private int med = 0;


    private static Bitmap getBitmap(Canvas canvas) {
        try {
            java.lang.reflect.Field field = Canvas.class.getDeclaredField("mBitmap");
            field.setAccessible(true);
            return (Bitmap) field.get(canvas);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout fl = (FrameLayout) findViewById(R.id.FrameLayout);
        CameraView cv = new CameraView(this);
        cv.setDataRotation(cv.DATA_ROTATION_AUTO);
        fl.addView(cv);
        calibrate = (Button) findViewById(R.id.calibrate);
        fseek = (SeekBar) findViewById(R.id.seekBar);
        dater = (TextView) findViewById(R.id.textView);
        calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (status == false) {
                    status = true;
                } else status = false;
                Log.d(TAG, "Status is " + status);
            }
        });
        fseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fault = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        cv.setCameraListener(new CameraListener() {
            @Override
            public void onCameraFrame(CameraData data, int cameraDisplayOrientation, Canvas canvas) {
                map = getBitmap(canvas);
                pcan = canvas;
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                int centerx = data.getWidth() / 2, centery = data.getHeight() / 2;
                y0 = centery;
                Paint blackPaint = new Paint();
                blackPaint.setColor(Color.BLACK);
                Paint redPaint = new Paint();
                redPaint.setColor(Color.RED);
                redPaint.setStyle(Paint.Style.STROKE);
                canvas.drawCircle(centerx, centery, 10, blackPaint);
                for (int y = 0; y < data.getHeight(); y += 20) {
                    for (int x = 0; x < data.getWidth(); x += 20) {
                        int color = data.getColor(centerx, centery);
                        int cColor = data.getColor(x, y);
                        int r = (color >> 16) & 0xFF;
                        int g = (color >> 8) & 0xFF;
                        int b = (color >> 0) & 0xFF;
                        //calibrate
                        if (status == true) {
                            RED = r;
                            GREEN = g;
                            BLUE = b;
                            Log.d(TAG, "Reference red is " + RED + " Reference green is " + GREEN + " Reference blue is " + BLUE);
                            status = false;
                        }
                        //processing
                        int cr = (cColor >> 16) & 0xFF;
                        int cg = (cColor >> 8) & 0xFF;
                        int cb = (cColor >> 0) & 0xFF;
                        if (cr < RED + fault && cr > RED - fault && cg < GREEN + fault && cg > GREEN - fault && cb < BLUE + fault && cb > BLUE - fault) {
                            figure.add(new Point(x, y));
                            count++;
                            canvas.drawRect(x, y, x + rectSize, y + rectSize, blackPaint);
                            canvas.drawRect(med, y0, med + 5, y0 + 200, redPaint);
                            save += x;
                        }
                    }
                }
            }

            @Override
            public void onCameraFrame(byte[] data, int width, int height, int cameraDisplayOrientation, Canvas canvas) {

            }

            @Override
            public void onSizeChange(int width, int height, int cameraDisplayOrientation) {

            }
        });

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Paint bp = new Paint();
                            bp.setStyle(Paint.Style.STROKE);
                            bp.setColor(Color.RED);
                            Point maxP = new Point(0, 0), minP = new Point(0, 0);
                            int size = figure.size();
                            for (int i = 0; i < size; i++) {
                                Point p = (Point) figure.get(i);
                                //Log.d(TAG, "Current x " + p.getX() + " Current y " + p.getY());
                                if (p.getX() > maxP.getX()) {
                                    maxP = p;
                                }
                                if (p.getY() > minP.getY()) {
                                    minP = p;
                                }
                            }
                            dater.setText("Max x " + maxP.getX() + " Max y " + maxP.getY() + " Min x " + minP.getX() + " Min y" + minP.getY());
                            if (map != null) {
                                //you can remove it:
                                pcan.drawRect(minP.getX(), minP.getY(), maxP.getX(), maxP.getY(), bp);
                                if (count != 0) {
                                    med = save / count;
                                }
                                // use this formula to compute controlling value: u = (int)(1.5*((int)((cord-y0/2)*100/height))-1);
                                Log.d(CTAG, "Current count value is " + count);
                                Log.d(CTAG, "Current save value is " + save);
                                Log.d(CTAG, "Current med value is " + med);
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

}