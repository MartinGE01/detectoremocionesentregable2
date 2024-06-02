package com.example.imagepro;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Core;

import java.io.IOException;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "CameraActivity";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;
    private objectDetectorClass objectDetectorClass;
    private boolean isFrontCamera = false;
    private boolean isFlashOn = false;
    private Camera camera;
    private Camera.Parameters params;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCv is loaded");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Request camera permission if not already granted
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            // Initialize the camera and parameters
            initializeCamera();
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView = findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        ImageButton rotateButton = findViewById(R.id.rotate_camera_button);
        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Acción para el botón de rotación de la cámara
                isFrontCamera = !isFrontCamera;
                mOpenCvCameraView.disableView();
                mOpenCvCameraView.setCameraIndex(isFrontCamera ? CameraBridgeViewBase.CAMERA_ID_FRONT : CameraBridgeViewBase.CAMERA_ID_BACK);
                mOpenCvCameraView.enableView();
                Toast.makeText(CameraActivity.this, "Cámara rotada", Toast.LENGTH_SHORT).show();
            }
        });

        ImageView flashButton = findViewById(R.id.imageViewFlash);
        flashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlash();
            }
        });

        try {
            // input size is 300 for this model
            objectDetectorClass = new objectDetectorClass(getAssets(), "ssd_mobilenet.tflite", "labelmap.txt", 300);
            Log.d("CameraActivity", "Model is successfully loaded");
        } catch (IOException e) {
            Log.d("CameraActivity", "Getting some error");
            e.printStackTrace();
        }
    }

    private void initializeCamera() {
        try {
            camera = Camera.open();
            params = camera.getParameters();
            Log.d(TAG, "Camera initialized");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing camera: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            // if load success
            Log.d(TAG, "Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            // if not loaded
            Log.d(TAG, "Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        // Release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        // Release the camera
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    private void toggleFlash() {
        if (camera == null) {
            Log.e(TAG, "Camera is null");
            return;
        }

        try {
            params = camera.getParameters();
            if (params == null) {
                Log.e(TAG, "Camera parameters are null");
                return;
            }

            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
                Log.e(TAG, "No flash feature available on this device");
                Toast.makeText(this, "Flash not available on this device", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isFlashOn) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);
                camera.stopPreview();
                isFlashOn = false;
                Toast.makeText(this, "Flash Off", Toast.LENGTH_SHORT).show();
            } else {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
                camera.startPreview();
                isFlashOn = true;
                Toast.makeText(this, "Flash On", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flash: " + e.getMessage());
            Toast.makeText(this, "Error toggling flash", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, initialize the camera
                initializeCamera();
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if (isFrontCamera) {
            Core.flip(mRgba, mRgba, 1); // Flip around Y axis to mirror the image
        }

        Mat out = new Mat();
        out = objectDetectorClass.recognizeImage(mRgba);

        return out;
    }
}
