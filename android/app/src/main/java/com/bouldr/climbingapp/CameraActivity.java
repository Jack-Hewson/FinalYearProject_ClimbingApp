package com.bouldr.climbingapp;

import android.Manifest;

import android.app.Fragment;
import android.content.Context;

import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.bouldr.climbingapp.ui.env.ImageUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.nio.ByteBuffer;

public abstract class CameraActivity extends androidx.fragment.app.Fragment
        implements OnImageAvailableListener,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    private static final int PERMISSIONS_REQUEST = 1;

    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private LinearLayout bottomSheetLayout;
    private LinearLayout gestureLayout;
    private BottomSheetBehavior<LinearLayout> sheetBehavior;

    protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
    protected ImageView bottomSheetArrowImageView;
    private ImageView plusImageView, minusImageView;
    private SwitchCompat apiSwitchCompat;
    private TextView threadsTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //  super.onCreate(null);
        View view = inflater.inflate(R.layout.tfe_od_activity_camera, container, false);

        //  getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (hasPermission()) {
            setFragment();
        } else {
            requestPermission();
        }

        threadsTextView = view.findViewById(R.id.threads);
        plusImageView = view.findViewById(R.id.plus);
        minusImageView = view.findViewById(R.id.minus);
        apiSwitchCompat = view.findViewById(R.id.api_info_switch);
        bottomSheetLayout = view.findViewById(R.id.bottom_sheet_layout);
        gestureLayout = view.findViewById(R.id.gesture_layout);
        sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetArrowImageView = view.findViewById(R.id.bottom_sheet_arrow);

        ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                            gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        //int width = bottomSheetLayout.getMeasuredWidth();
                        int height = gestureLayout.getMeasuredHeight();

                        sheetBehavior.setPeekHeight(height);
                    }
                });
        sheetBehavior.setHideable(false);

        sheetBehavior.setBottomSheetCallback(
                new BottomSheetBehavior.BottomSheetCallback() {
                    @Override
                    public void onStateChanged(@NonNull View bottomSheet, int newState) {
                        switch (newState) {
                            case BottomSheetBehavior.STATE_HIDDEN:
                                break;
                            case BottomSheetBehavior.STATE_EXPANDED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                            }
                            break;
                            case BottomSheetBehavior.STATE_COLLAPSED: {
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                            }
                            break;
                            case BottomSheetBehavior.STATE_DRAGGING:
                                break;
                            case BottomSheetBehavior.STATE_SETTLING:
                                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                                break;
                        }
                    }

                    @Override
                    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    }
                });

        frameValueTextView = view.findViewById(R.id.frame_info);
        cropValueTextView = view.findViewById(R.id.crop_info);
        inferenceTimeTextView = view.findViewById(R.id.inference_info);

        apiSwitchCompat.setOnCheckedChangeListener(this);

        plusImageView.setOnClickListener(this);
        minusImageView.setOnClickListener(this);

        return view;
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
        }
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                requestPermission();
            }
        }
    }

    private static boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getActivity().checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
        }
    }

    private String chooseCamera() {
        final CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (final String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                final StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                if (map == null) {
                    continue;
                }
                return cameraId;
            }
        } catch (CameraAccessException e) {
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();

        Fragment fragment;
        CameraConnectionFragment camera2Fragment =
                CameraConnectionFragment.newInstance(
                        new CameraConnectionFragment.ConnectionCallback() {
                            @Override
                            public void onPreviewSizeChosen(final Size size, final int rotation) {
                                previewHeight = size.getHeight();
                                previewWidth = size.getWidth();
                                CameraActivity.this.onPreviewSizeChosen(size, rotation);
                            }
                        },
                        this,
                        getLayoutId(),
                        getDesiredPreviewFrameSize());

        camera2Fragment.setCamera(cameraId);
        fragment = camera2Fragment;

        getActivity().getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                //   LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getActivity().getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setUseNNAPI(isChecked);
        if (isChecked) apiSwitchCompat.setText("NNAPI");
        else apiSwitchCompat.setText("TFLITE");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.plus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads >= 9) return;
            numThreads++;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        } else if (v.getId() == R.id.minus) {
            String threads = threadsTextView.getText().toString().trim();
            int numThreads = Integer.parseInt(threads);
            if (numThreads == 1) {
                return;
            }
            numThreads--;
            threadsTextView.setText(String.valueOf(numThreads));
            setNumThreads(numThreads);
        }
    }

    protected void showFrameInfo(String frameInfo) {
        frameValueTextView.setText(frameInfo);
    }

    protected void showCropInfo(String cropInfo) {
        cropValueTextView.setText(cropInfo);
    }

    protected void showInference(String inferenceTime) {
        inferenceTimeTextView.setText(inferenceTime);
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();

    protected abstract void setNumThreads(final int numThreads);

    protected abstract void setUseNNAPI(final boolean isChecked);

}
