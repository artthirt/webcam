package com.example.webcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
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
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

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

    private ToggleButton mCamToggle = null;
    private RadioButton mCamBackRb = null;
    private RadioButton mCamFrontRb = null;

    private TextView mTextView = null;

    private Menu mMenu = null;

    private Timer mTimer = new Timer();

    private SharedPreferences mPrefs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        //params.screenBrightness = 0;
        getWindow().setAttributes(params);

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

        mCamToggle = (ToggleButton) findViewById(R.id.toggle_camera);

        mCamBackRb = (RadioButton)findViewById(R.id.rb_back);
        mCamFrontRb = (RadioButton)findViewById(R.id.rb_front);

        mTextView = (TextView)findViewById(R.id.text_view);

        Display d = getWindowManager().getDefaultDisplay();
        Point sz = new Point();
        d.getSize(sz);
        mTextView.setHeight(sz.y - 150);

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

                int fmts[] = confMap.getOutputFormats();
                for(int fmt: fmts){
                    Log.i(LOG_TAG, "output format " + fmt);
                }

                mCameras[id] = new CameraService(this, mCameraManager, camId, mImageView);
                mCameras[id].sizes = sizesJPEG;
            }

            mPrefs = getSharedPreferences("config", Context.MODE_PRIVATE);

            CameraService.HOST = mPrefs.getString("ip", "10.0.2.2");
            CameraService.PORT = mPrefs.getInt("port", 8000);
            CameraService.BITRATE = mPrefs.getInt("bitrate", CameraService.BITRATE);
            CameraService.QUALITY_JPEG = mPrefs.getInt("quality.jpeg", CameraService.QUALITY_JPEG);
            CameraService.USE_PREVIEW = mPrefs.getBoolean("use_preview", CameraService.USE_PREVIEW);
            CameraService.FRAMERATE = mPrefs.getInt("framerate", CameraService.FRAMERATE);

            mCameras[CAMERA_FRONT].setCurrentSizeIndex(mPrefs.getInt("frontcamindex", -1));
            mCameras[CAMERA_BACK].setCurrentSizeIndex(mPrefs.getInt("backcamindex", -1));
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mCamToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCamToggle.isChecked()){
                    if(mCamBackRb.isChecked()){
                        if(mCameras[CAMERA_BACK] != null){
                            if(mCameras[CAMERA_BACK].isOpen())
                                mCameras[CAMERA_BACK].closeCamera();
                            mCameras[CAMERA_BACK].openCamera();
                        }
                    }
                    if(mCamFrontRb.isChecked()){
                        if(mCameras[CAMERA_BACK].isOpen())
                            mCameras[CAMERA_BACK].closeCamera();

                        if(mCameras[CAMERA_FRONT] != null){
                            if(mCameras[CAMERA_FRONT].isOpen())
                                mCameras[CAMERA_FRONT].closeCamera();
                            mCameras[CAMERA_FRONT].openCamera();
                        }
                    }
                }else{
                    mCameras[CAMERA_FRONT].closeCamera();
                    mCameras[CAMERA_BACK].closeCamera();
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
        mTextView.setText("CAMERA BACK:      frames count " + mCameras[CAMERA_BACK].getFramesCount() + "\n" +
                "CAMERA FRONT:   frames count " + mCameras[CAMERA_FRONT].getFramesCount() + "\n" + error);


        if(CameraService.USE_PREVIEW && mImageView != null){
            int w = 0, h = 0;
            if(mCameras[CAMERA_BACK].isOpen()){
                w = mCameras[CAMERA_BACK].widthImage;
                h = mCameras[CAMERA_BACK].heightImage;
            }
            if(mCameras[CAMERA_FRONT].isOpen()){
                w = mCameras[CAMERA_FRONT].widthImage;
                h = mCameras[CAMERA_FRONT].heightImage;
            }
            if(w != 0 && h != 0){
                Matrix tx = new Matrix();
                int wv = mImageView.getWidth();
                int hv = mImageView.getHeight();

                float ari = (float)h/w;
                float arw = (float)wv/hv;

                mImageView.getTransform(tx);

                if(ari < arw){
                    tx.setScale((float)1, (float)ari / arw);
                }else{
                    tx.setScale((float)ari/arw, (float)1);
                }

                float xoff = 0;
                float yoff = hv/2 - (hv * ari / arw)/2;
                tx.postTranslate(xoff, yoff);
                mImageView.setTransform(tx);
            }
        }
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
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

                SettingsDialog sd = new SettingsDialog(this, this);
                sd.setSizesFrontCamera(mCameras[CAMERA_FRONT].sizes, mCameras[CAMERA_FRONT].getCurrentSizeIndex());
                sd.setSizesBackCamera(mCameras[CAMERA_BACK].sizes, mCameras[CAMERA_BACK].getCurrentSizeIndex());
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

    public void updateConfig(){
        if(mPrefs != null){
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString("ip", CameraService.HOST);
            editor.putInt("port", CameraService.PORT);
            editor.putInt("quality.jpeg", CameraService.QUALITY_JPEG);
            editor.putInt("bitrate", CameraService.BITRATE);
            editor.putInt("framerate", CameraService.FRAMERATE);
            editor.putBoolean("use_preview", CameraService.USE_PREVIEW);
            editor.putInt("frontcamindex", mCameras[CAMERA_FRONT].getCurrentSizeIndex());
            editor.putInt("backcamindex", mCameras[CAMERA_BACK].getCurrentSizeIndex());
            editor.commit();
        }
    }

    public void setFrontCameraIndex(int id){
        mCameras[CAMERA_FRONT].setCurrentSizeIndex(id);
    }
    public void setBackCameraIndex(int id){
        mCameras[CAMERA_BACK].setCurrentSizeIndex(id);
    }

    public void setTypeEncoding(int v){
        if(mCameras[CAMERA_BACK] != null){
            mCameras[CAMERA_BACK].setTypeEncode(v == 0? CameraService.CHOOSE_JPEG : CameraService.CHOOSE_H264);
        }
        if(mCameras[CAMERA_FRONT] != null) {
            mCameras[CAMERA_FRONT].setTypeEncode(v == 0? CameraService.CHOOSE_JPEG : CameraService.CHOOSE_H264);
        }
    }
    public void setUsePreview(boolean u){
        if(mCameras[CAMERA_BACK] != null){
            mCameras[CAMERA_BACK].setUsePreview(u);
        }
        if(mCameras[CAMERA_FRONT] != null) {
            mCameras[CAMERA_FRONT].setUsePreview(u);
        }
    }
}
