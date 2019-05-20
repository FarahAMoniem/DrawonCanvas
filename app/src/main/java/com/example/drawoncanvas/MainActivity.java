package com.example.drawoncanvas;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

//import android.R;
public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    SurfaceView cameraView, transparentView;
    SurfaceHolder holder, holderTransparent;
    Camera camera;
    private static final int PERMISSION_REQUEST_CODE = 200;

    private float RectLeft, RectTop, RectRight, RectBottom;
    int deviceHeight, deviceWidth;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (checkPermission()) {
            cameraView = (SurfaceView) findViewById(R.id.CameraView);
            holder = cameraView.getHolder();
            holder.addCallback((SurfaceHolder.Callback) this);
            //holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            cameraView.setSecure(true);
            // Create second surface with another holder (holderTransparent)
            transparentView = (SurfaceView) findViewById(R.id.TransparentView);
            holderTransparent = transparentView.getHolder();
            holderTransparent.addCallback((SurfaceHolder.Callback) this);
            holderTransparent.setFormat(PixelFormat.TRANSLUCENT);
            transparentView.setZOrderMediaOverlay(true);
            //getting the device heigth and width
            deviceWidth = getScreenWidth();
            deviceHeight = getScreenHeight();

        } else {
            requestPermission();
        }

    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }


    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }
        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    private void Draw() {
        Canvas canvas = holderTransparent.lockCanvas(null);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5);
        RectLeft = 1;
        RectTop = 200;
        RectRight = RectLeft + deviceWidth - 100;
        RectBottom = RectTop + 200;
        Rect rec = new Rect((int) RectLeft, (int) RectTop, (int) RectRight, (int) RectBottom);
      //  canvas.drawRect(rec, paint);
        canvas.drawCircle( RectLeft + deviceWidth/2 ,RectTop + 200,200,paint);
        holderTransparent.unlockCanvasAndPost(canvas);
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            synchronized (holder) {
                Draw();
            }   //call a draw method
            Log.i( "num:","ok "+Camera.getNumberOfCameras());
            camera = android.hardware.Camera.open(findBackFacingCamera()); //open a camera
        } catch (Exception e) {
            Log.i("Exception", e.toString());
            return;
        }
        Camera.Parameters param;
        param = camera.getParameters();
      //  param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_0) {
            camera.setDisplayOrientation(90);
        }
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
            return;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        refreshCamera(); //call method for refress camera
    }
    public void refreshCamera() {
        if (holder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
        } catch (Exception e) {
        }
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (Exception e) {
        }
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.release(); //for release a camera
    }

    private int findBackFacingCamera() {
        int cameraId=0;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                //cameraFront = true;
                break;
            }
        }
        return cameraId;
    }
}