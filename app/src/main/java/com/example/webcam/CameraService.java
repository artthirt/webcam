package com.example.webcam;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.example.webcam.MainActivity.LOG_TAG;

public class CameraService {

    static{
        System.loadLibrary("sendasf");
    }

    public static native int senddata(byte[] data, String ip, int port);

    private String mCamId;
    private CameraDevice mCamDev = null;
    private CameraManager mCamManager = null;
    private Context mContex;
    private CameraCaptureSession mCaptureSession = null;
    private TextureView mTexView;
    private Handler mHandler = null;
    private CaptureRequest.Builder mBuilder = null;
    private boolean mIsOpen = false;
    private boolean mIsError = false;
    private boolean mIsRestart = false;

    private HandlerThread mImageReaderHandlerThread = null;
    private Handler mImageReaderHandler = null;

    private int mFramesCount = 0;

    private String mIP = "10.0.2.2";
    private int mPort = 1234;

    public void setNetwork(String IP, int port){
        mIP = IP;
        mPort = port;
    }

    public int getFramesCount(){
        return mFramesCount;
    }

    public boolean isError(){
        return mIsError;
    }

    public Size sizes[] = null;

    private CameraDevice.StateCallback mCamCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamDev = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamDev.close();
            mCamDev = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(LOG_TAG, "error " + error);
            mIsError = true;
            if(mIsOpen)
                mIsRestart = true;
        }
    };

    private void reOpenCamera() {
        mIsRestart = false;
        closeCamera();
        openCamera();
    }

    public Timer mTimer = new Timer();

    public CameraService(Context context, CameraManager camManager, String camId, TextureView texView){
        mCamManager = camManager;
        mCamId = camId;
        mContex = context;
        mTexView = texView;

        //byte data[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
        //int len = senddata(data);
        //Log.i(LOG_TAG, "len " + len);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(mIsRestart){
                    reOpenCamera();
                }
            }
        }, 1000, 1000);

        mImageReaderHandlerThread = new HandlerThread("ImageReaderHandlerThread" + camId);
        mImageReaderHandlerThread.start();
        mImageReaderHandler = new Handler(mImageReaderHandlerThread.getLooper());
    }

    public void setHandler(Handler handler){
        mHandler = handler;
    }

    public boolean isOpen(){
        if(mCamDev == null)
            return false;
        return true;
    }

    public void openCamera(){
        try{
            if(checkSelfPermission(mContex,  Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                mCamManager.openCamera(mCamId, mCamCallback, mHandler);
                mIsOpen = true;
            }
        }catch (CameraAccessException e){
            Log.i(LOG_TAG, e.getMessage());
            mIsError = true;
        }
    }

    public void closeCamera(){
        if(mCamDev != null){
            mCamDev.close();
            mCamDev = null;
        }
        mIsOpen = false;
    }

    private Surface mSurface = null;
    private ImageReader mImageReader = null;

    private void createCameraPreviewSession(){

        Size s = null;

        if(sizes != null) {
            Size sout = sizes[0];
            for (Size s1 : sizes) {
                if(s1.getWidth() == 1920 && s1.getHeight() == 1080){
                    s = s1;
                    break;
                }
                if(s1.getWidth() > sout.getWidth() || s1.getHeight() > sout.getHeight()){
                    sout = s1;
                }
            }
            if(s == null)
                s = sout;
        }else{
            s = new Size(640, 480);
        }

        mImageReader = ImageReader.newInstance(s.getWidth(), s.getHeight(), ImageFormat.YUV_420_888, 5);
        mImageReader.setOnImageAvailableListener(mImageCaptureListener, mImageReaderHandler);

        SurfaceTexture texture = mTexView.getSurfaceTexture();
        mSurface = new Surface(texture);

        try{
            mBuilder = mCamDev.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mBuilder.addTarget(mSurface);
            mBuilder.addTarget(mImageReader.getSurface());

            mCamDev.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    mBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    try{
                        mCaptureSession.setRepeatingRequest(mBuilder.build(), mCaptureCallback, mHandler);
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mHandler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.i(LOG_TAG, "capture completed");
        }

        @Override
        public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
            super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }
    };

    final ImageReader.OnImageAvailableListener mImageCaptureListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            mHandler.post(new CapturedImageSaver(reader.acquireNextImage(), mIP, mPort));
            mFramesCount++;
        }
    };

    static int mCurrentThreads = 0;
    static int mMaxThreads = 20;

    static class CapturedImageSaver implements Runnable{
        private Image mCapture;
        private String mIP;
        private int mPort;

        public CapturedImageSaver(Image capture, String ip, int port)
        {
            mCapture = capture;
            mIP = ip;
            mPort = port;
        }

        public void run(){
            ByteBuffer buffer = mCapture.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            ByteBuffer buffer1 = mCapture.getPlanes()[1].getBuffer();
            byte[] bytes1 = new byte[buffer1.remaining()];

            ByteBuffer buffer2 = mCapture.getPlanes()[2].getBuffer();
            byte[] bytes2 = new byte[buffer2.remaining()];

            Log.i(LOG_TAG, "size " + mCapture.getWidth() + "x" + mCapture.getHeight() +
                    ", planes " + mCapture.getPlanes().length +
                    ", plane0 " + bytes.length +
                    ", plane1 " + bytes1.length +
                    ", plane2 " + bytes2.length +
                    ", " + bytes.length + ", currentThreads " + mCurrentThreads);
            //senddata(bytes, mIP, mPort);
//            if(mCurrentThreads < mMaxThreads) {
//                mCurrentThreads++;
//                new SenderTask(mIP, mPort).execute(bytes);
//            }
            mCapture.close();
        }
    };

    static class SenderTask extends AsyncTask<byte[], Void, Void>{

        private String mIP;
        private int mPort;

        public SenderTask(String ip, int port){
            mIP = ip;
            mPort = port;
        }

        @Override
        protected Void doInBackground(byte[]... bytes) {
            senddata(bytes[0], mIP, mPort);
            Log.i(LOG_TAG, "size " + bytes[0].length);
            mCurrentThreads--;
            return null;
        }
    }
}
