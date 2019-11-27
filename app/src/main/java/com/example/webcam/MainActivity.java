package com.example.webcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "WCLOG";
    private static int CAMERA_BACK = 0;
    private static int CAMERA_FRONT = 1;
    private CameraService[] mCameras = null;
    private CameraManager mCameraManager = null;

    private HandlerThread mBackgroundThread = null;
    private Handler mBackgroundHandler = null;

    private TextureView mImageView = null;

    private Button mCamFront = null;
    private Button mCamBack = null;

    private TextView mTextView = null;

    private Menu mMenu = null;

    private Timer mTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                ||
                checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                ||
                (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        )
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);
        }

        mCameraManager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);

        mCamFront = (Button)findViewById(R.id.button_open_front);
        mCamBack = (Button)findViewById(R.id.button_open_back);

        mTextView = (TextView)findViewById(R.id.text_view);

        mImageView = (TextureView)findViewById(R.id.texture_view);

        try {
            mCameras = new CameraService[mCameraManager.getCameraIdList().length];

            int idcnt = 0;
            for(String camId: mCameraManager.getCameraIdList()){
                Log.i(LOG_TAG, camId);
                int id = Integer.parseInt(camId);

                CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(camId);
                StreamConfigurationMap confMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int Faceing = cc.get(CameraCharacteristics.LENS_FACING);
                if(Faceing == CameraCharacteristics.LENS_FACING_FRONT){
                    Log.i(LOG_TAG, camId + " is Front Camera");
                }else if(Faceing == CameraCharacteristics.LENS_FACING_BACK){
                    Log.i(LOG_TAG, camId + " is Back Camera");
                }

                Size[] sizesJPEG = confMap.getOutputSizes(ImageFormat.JPEG);
                if(sizesJPEG != null){
                    for(Size it: sizesJPEG){
                        Log.i(LOG_TAG, "size=" + it.getWidth() + "x" + it.getHeight());
                    }
                }else{
                    Log.i(LOG_TAG, "Camera do not support JPEG");
                }

                mCameras[id] = new CameraService(this, mCameraManager, camId, mImageView);
                mCameras[id].sizes = sizesJPEG;
            }

            setNetworkConfig(mIP, mPort);
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mCamBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameras[CAMERA_FRONT].isOpen())
                    mCameras[CAMERA_FRONT].closeCamera();

                if(mCameras[CAMERA_BACK] != null){
                    if(mCameras[CAMERA_BACK].isOpen())
                        mCameras[CAMERA_BACK].closeCamera();
                    mCameras[CAMERA_BACK].openCamera();
                }
            }
        });

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                doTimer();
            }
        }, 1000, 1000);
    }

    private void doTimer() {
        if(mTextView == null)
            return;

        String error = "";
        if(mCameras[CAMERA_BACK].isError()){
            error = "CAMERA BACK got error";
        }
        mTextView.setText("CAMERA BACK: frames count " + mCameras[CAMERA_BACK].getFramesCount() + "\n" +
                "CAMERA FRONT: frames count " + mCameras[CAMERA_FRONT].getFramesCount() + "\n" + error);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgrtoundThread(){
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        mCameras[CAMERA_BACK].setHandler(mBackgroundHandler);
        mCameras[CAMERA_FRONT].setHandler(mBackgroundHandler);
    }

    @Override
    public void onPause() {
        super.onPause();

        stopBackgrtoundThread();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        mMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings: {
//                Intent intent = new Intent(this, ServiceConnect.class);
//                startService(intent);

                SettingsDialog sd = new SettingsDialog(this, this, mIP, mPort);
                sd.show();
                return true;
            }
            case R.id.action_exit:{
//                Intent intent = new Intent(this, ServiceConnect.class);
//                stopService(intent);
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private String mIP = "10.0.3.2";
    private int mPort = 8000;

    public void setNetworkConfig(String ip, int port){
        mIP = ip;
        mPort = port;

        if(mCameras[CAMERA_BACK] != null){
            mCameras[CAMERA_BACK].setNetwork(mIP, mPort);
        }
        if(mCameras[CAMERA_FRONT] != null){
            mCameras[CAMERA_FRONT].setNetwork(mIP, mPort);
        }
    }
}
