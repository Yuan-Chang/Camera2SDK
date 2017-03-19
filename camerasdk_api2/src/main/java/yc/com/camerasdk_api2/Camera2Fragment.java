package yc.com.camerasdk_api2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;

public class Camera2Fragment extends Fragment {

    public enum ReturnType{
        FileBytes,
        YUV,
        Bitmap
    }

    private ReturnType returnType;

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private String cameraId;
    private ImageReader imageReader;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private TextureView.SurfaceTextureListener surfaceTextureListener;

    private TextureView textureView;
    private Button takePicBtn;

    /**
     * Find the Views in the layout<br />
     * <br />
     * Auto-created on 2017-02-26 19:33:41 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    private void findViews(View view) {
        textureView = (TextureView)view.findViewById( R.id.textureView );
        takePicBtn = (Button)view.findViewById( R.id.takePicBtn );
    }



    public Camera2Fragment() {
        // Required empty public constructor
    }

    void init()
    {
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //start the camera
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera2, container, false);
        findViews(view);

        init();

        return view;
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            //get camera info
            StreamConfigurationMap cameraConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert cameraConfig != null;

            //get one available size
            imageDimension = cameraConfig.getOutputSizes(SurfaceTexture.class)[0];

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    //This is called when the camera is open
                    Log.e(TAG, "onOpened");
                    cameraDevice = camera;

                    //start preview
                    startCameraPreview();
                }
                @Override
                public void onDisconnected(CameraDevice camera) {
                    cameraDevice.close();
                }
                @Override
                public void onError(CameraDevice camera, int error) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    void startCameraPreview() {
        try {

            SurfaceTexture texture = textureView.getSurfaceTexture();

            if (texture == null || cameraDevice == null)
                return;

            HandlerThread thread = new HandlerThread("");
            thread.start();
            final Handler handler = new Handler(thread.getLooper());

            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());

            Surface surface = new Surface(texture);
            //set capture request as 'preview' mode
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);


            CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] imageSize = null;
            if (characteristics != null) {
                imageSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.YUV_420_888);
            }
            int width = 640;
            int height = 480;
            if (imageSize != null && 0 < imageSize.length) {
                width = imageSize[0].getWidth();
                height = imageSize[0].getHeight();
            }

            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>();
            outputSurfaces.add(imageReader.getSurface());
            outputSurfaces.add(surface);

            //set output target
            captureRequestBuilder.addTarget(surface);
            captureRequestBuilder.addTarget(imageReader.getSurface());


            final int fwidth = width;
            final int fheight = height;
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        if (imageLiveFrameListener != null)
                            imageLiveFrameListener.onImageLiveFrame(bytes,fwidth,fheight);

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, handler);

            //create session
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // session is created
                    cameraCaptureSessions = cameraCaptureSession;

                    //1st param => set up camera display effect( ex. it can be set to black & while)
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    try {
                        cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getActivity(), "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        int imageFormat = ImageFormat.JPEG;

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] imageSize = null;
            if (characteristics != null) {
                imageSize = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(imageFormat);
            }
            int width = 640;
            int height = 480;
            if (imageSize != null && 0 < imageSize.length) {
                width = imageSize[0].getWidth();
                height = imageSize[0].getHeight();
            }

            imageReader = ImageReader.newInstance(width, height, imageFormat, 1);

            List<Surface> outputSurfaces = new ArrayList<Surface>();
            outputSurfaces.add(imageReader.getSurface());

            CaptureRequest.Builder captureBuilder1 = null;
            try{
                captureBuilder1 = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            }
            catch (CameraAccessException e)
            {
                Log.d("yuan","camera exceiption "+e.toString());
            }
            final CaptureRequest.Builder captureBuilder = captureBuilder1;

            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            int rotation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);

            final int imageWidth = width;
            final int imageHeight = height;
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        if (imageTakenListener != null)
                            imageTakenListener.onImageTakenBytes(bytes,imageWidth,imageHeight);

                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            }, null);

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                                super.onCaptureCompleted(session, request, result);
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public void setReturnType(ReturnType returnType) {
        this.returnType = returnType;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(getActivity(), "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
        }
    }

    OnImageTakenListener imageTakenListener;

    public interface OnImageTakenListener{
        void onImageTakenBytes(byte[] bytes, int width, int height);
    }

    public void setOnImageTakenBytesListener(OnImageTakenListener onImageTakenListener){
        imageTakenListener = onImageTakenListener;
    }

    OnImageLiveFrameListener imageLiveFrameListener;

    public interface OnImageLiveFrameListener{
        void onImageLiveFrame(byte[] bytes, int width, int height);
    }

    public void setOnImageLiveFrameListener(OnImageLiveFrameListener onImageLiveFrameListener){
        imageLiveFrameListener = onImageLiveFrameListener;
    }
}
