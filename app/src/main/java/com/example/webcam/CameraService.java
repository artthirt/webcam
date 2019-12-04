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
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.example.webcam.MainActivity.LOG_TAG;

public class CameraService {

    static{
        System.loadLibrary("sendasf");
    }

    public static native int senddata(byte[][] data, int width, int height, String ip, int port);
    public static native int senddata_jpeg(byte[] data, String ip, int port);

    public static int CHOOSE_JPEG = 0;
    public static int CHOOSE_H264 = 1;

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

    public int widthImage = 0;
    public int heightImage = 0;

    private HandlerThread mImageReaderHandlerThread = null;
    private Handler mImageReaderHandler = null;

    private int mFramesCount = 0;

    private int mTypeEncode = CHOOSE_H264;

    private Surface mEncodeSurface = null;
    private int mMediaFrameKeyCount = 0;
    private int mMediaFrameKeyCountMax = FRAMERATE;
    private int mCurrentSizeIndex = -1;

    public static int BITRATE = 20000000;
    public static int FRAMERATE = 30;
    public static String HOST = "10.0.2.2";
    public static int PORT = 8000;
    public static int QUALITY_JPEG = 50;
    public static boolean USE_PREVIEW = false;

    public void setTypeEncode(int enc){
        mTypeEncode = enc;
    }

    public void setUsePreview(boolean v){
        USE_PREVIEW = v;

        if(isOpen()){
            closeCamera();
            openCamera();
        }
    }

    public int getFramesCount(){
        return mFramesCount;
    }

    public boolean isError(){
        return mIsError;
    }

    public Size sizes[] = null;

    public void setCurrentSizeIndex(int id){
        if(sizes == null)
            return;
        if(id >= 0 && id < sizes.length) {
            mCurrentSizeIndex = id;

            Size s = sizes[id];
            Log.i(LOG_TAG, "" + s.getWidth() + "x" + s.getHeight());
        }
    }
    public int getCurrentSizeIndex(){
        return mCurrentSizeIndex;
    }

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
        if(mMediaCodec != null){
            mMediaCodec.stop();
            mMediaCodec.release();;
        }
        if(mCamDev != null){
            mCamDev.close();
            mCamDev = null;
        }
        mIsOpen = false;
    }

    private Surface mSurface = null;
    private ImageReader mImageReader = null;
    private MediaCodec mMediaCodec = null;
    private byte[] mCodeConfigBytes = null;

    private void createCameraPreviewSession(){

        Size s = null;

        if(sizes != null) {
            if(mCurrentSizeIndex < 0) {
                Size sout = sizes[0];
                for (Size s1 : sizes) {
                    if (s1.getWidth() == 1920 && s1.getHeight() == 1080) {
                        s = s1;
                        break;
                    }
                    if (s1.getWidth() > sout.getWidth() || s1.getHeight() > sout.getHeight()) {
                        sout = s1;
                    }
                }
                if (s == null)
                    s = sout;
            }else{
                s = sizes[mCurrentSizeIndex];
            }
        }else{
            s = new Size(640, 480);
        }

        SurfaceTexture texture = mTexView.getSurfaceTexture();
        mSurface = new Surface(texture);

        try{
            mBuilder = mCamDev.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            if(USE_PREVIEW)
                mBuilder.addTarget(mSurface);

            mBuilder.set(CaptureRequest.JPEG_QUALITY, (byte)QUALITY_JPEG);

            List<Surface> inputSurf = null;

            widthImage = s.getWidth();
            heightImage = s.getHeight();
            if(mTypeEncode == CHOOSE_JPEG) {
                mImageReader = ImageReader.newInstance(s.getWidth(), s.getHeight(), ImageFormat.JPEG, 5);
                mImageReader.setOnImageAvailableListener(mImageCaptureListener, mImageReaderHandler);
                if(USE_PREVIEW) {
                    inputSurf = Arrays.asList(mSurface, mImageReader.getSurface());
                }else{
                    inputSurf = Arrays.asList(mImageReader.getSurface());
                }
                mBuilder.addTarget(mImageReader.getSurface());
            }else{
                mMediaCodec = MediaCodec.createEncoderByType("video/avc");
                MediaFormat format = MediaFormat.createVideoFormat("video/avc", s.getWidth(), s.getHeight());
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, BITRATE);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAMERATE);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
                Log.i(LOG_TAG, "format framerate " + FRAMERATE);
                mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mMediaCodec.setCallback(mMediaCallback);
                mEncodeSurface = mMediaCodec.createInputSurface();
                mMediaCodec.start();
                if(USE_PREVIEW) {
                    inputSurf = Arrays.asList(mSurface, mEncodeSurface);
                }else{
                    inputSurf = Arrays.asList(mEncodeSurface);
                }
                mBuilder.addTarget(mEncodeSurface);
            }
            mCamDev.createCaptureSession(inputSurf, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCaptureSession = session;
                    mBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    try{
                        mCaptureSession.setRepeatingRequest(mBuilder.build(), null, mHandler);
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
        }catch (IOException e){
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
            mHandler.post(new CapturedImageSaver(reader.acquireNextImage(), HOST, PORT));
            mFramesCount++;
        }
    };

    static int mCurrentThreads = 0;
    static int mMaxThreads = 3;

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

//            ByteBuffer buffer1 = mCapture.getPlanes()[1].getBuffer();
//            byte[] bytes1 = new byte[buffer1.remaining()];
//
//            ByteBuffer buffer2 = mCapture.getPlanes()[2].getBuffer();
//            byte[] bytes2 = new byte[buffer2.remaining()];

//            Log.i(LOG_TAG, "size " + mCapture.getWidth() + "x" + mCapture.getHeight() +
//                    ", planes " + mCapture.getPlanes().length +
//                    ", plane0 " + bytes.length +
////                    ", plane1 " + bytes1.length +
////                    ", plane2 " + bytes2.length +
//                    ", " + bytes.length + ", currentThreads " + mCurrentThreads);
            //senddata(bytes, mIP, mPort);
            if(mCurrentThreads < mMaxThreads) {
                mCurrentThreads++;
//                byte[][] planes = {bytes, bytes1, bytes2};
                new SenderTask(mCapture.getWidth(), mCapture.getHeight(), mIP, mPort).execute(bytes);
            }
            mCapture.close();
        }
    };

    private MediaCodec.Callback mMediaCallback = new MediaCodec.Callback(){

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            Log.i(LOG_TAG, "available");
//            ByteBuffer inputBuffer = codec.getInputBuffer(index);
//            int inputBufferIndex = codec.dequeueInputBuffer(10000);
//            if(inputBufferIndex >= 0){
//                codec.queueInputBuffer(inputBufferIndex, 0, inputBuffer.limit(), 10000,
//                        MediaCodec.BUFFER_FLAG_KEY_FRAME);
//            }
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            ByteBuffer buffer = mMediaCodec.getOutputBuffer(index);
            byte data[] = new byte[buffer.remaining()];
            buffer.get(data);

            boolean isConfigFrame = (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
            if(isConfigFrame) {
                Log.i(LOG_TAG, "Config frame generated");
                mCodeConfigBytes = data;
            }

            if(mCurrentThreads < mMaxThreads) {
                mCurrentThreads++;
//                byte[][] planes = {bytes, bytes1, bytes2};
                new SenderTask(0, 0, HOST, PORT).execute(data);
            }

            mMediaCodec.releaseOutputBuffer(index, false);
            mFramesCount++;

            mMediaFrameKeyCount++;

            if(mMediaFrameKeyCount > mMediaFrameKeyCountMax) {
                Bundle params = new Bundle();
                params.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
                mMediaCodec.setParameters(params);

                mMediaFrameKeyCount = 0;
                new SenderTask(0, 0, HOST, PORT).execute(mCodeConfigBytes);
            }
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.i(LOG_TAG, "Error " + e);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

        }
    };

    static class SenderTask extends AsyncTask<byte[], Void, Void>{

        private String mIP;
        private int mPort;
        private int mW;
        private int mH;

        public SenderTask(int w, int h, String ip, int port){
            mIP = ip;
            mPort = port;
            mW = w;
            mH = h;
        }

        @Override
        protected Void doInBackground(byte[]... bytes) {
            senddata_jpeg(bytes[0], mIP, mPort);
            //Log.i(LOG_TAG, "size " + bytes[0].length);
            mCurrentThreads--;
            return null;
        }
    }
}
