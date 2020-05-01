package com.example.climbingapp;

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

import com.example.climbingapp.ui.env.ImageUtils;
import com.example.climbingapp.ui.env.Logger;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.nio.ByteBuffer;

public abstract class CameraActivity extends androidx.fragment.app.Fragment
        implements OnImageAvailableListener,
        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
    private static final Logger LOGGER = new Logger();

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

            // LOGGER.i("width = " + previewWidth + " height = " + previewHeight);

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
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }

    @Override
    public synchronized void onStart() {
        LOGGER.d("onStart " + this);
        super.onStart();
    }

    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
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
            LOGGER.e(e, "Not allowed to access camera");
        }

        return null;
    }

    protected void setFragment() {
        String cameraId = chooseCamera();
        LOGGER.i("cameraId = " + cameraId);

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

    /**protected void processImage() {
     ++timestamp;
     final long currTimestamp = timestamp;
     trackingOverlay.postInvalidate();

     // No mutex needed as this method is not reentrant.
     if (computingDetection) {
     readyForNextImage();
     return;
     }
     computingDetection = true;
     // LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

     rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

     readyForNextImage();

     final Canvas canvas = new Canvas(croppedBitmap);
     canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
     // For examining the actual TF input.
     if (SAVE_PREVIEW_BITMAP) {
     ImageUtils.saveBitmap(croppedBitmap);
     }

     runInBackground(
     new Runnable() {
    @Override public void run() {
    // LOGGER.i("Running detection on image " + currTimestamp);
    final long startTime = SystemClock.uptimeMillis();
    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
    LOGGER.i("RESULTS = " + results);
    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
    final Canvas canvas = new Canvas(cropCopyBitmap);
    final Paint paint = new Paint();
    paint.setColor(Color.RED);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2.0f);

    float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
    switch (MODE) {
    case TF_OD_API:
    minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
    break;
    }

    final List<Classifier.Recognition> mappedRecognitions =
    new LinkedList<Classifier.Recognition>();
    for (final Classifier.Recognition result : results) {
    final RectF location = result.getLocation();
    if (location != null && result.getConfidence() >= minimumConfidence) {
    canvas.drawRect(location, paint);

    cropToFrameTransform.mapRect(location);

    result.setLocation(location);
    mappedRecognitions.add(result);
    }
    }

    tracker.trackResults(mappedRecognitions, currTimestamp);
    trackingOverlay.postInvalidate();

    computingDetection = false;
    getActivity().runOnUiThread(
    new Runnable() {
    @Override public void run() {
    showFrameInfo(previewWidth + "x" + previewHeight);
    showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
    showInference(lastProcessingTimeMs + "ms");
    }
    });
    }
    });
     }
     */
/**
 public void onPreviewSizeChosen(final Size size, final int rotation) {
 final float textSizePx =
 TypedValue.applyDimension(
 TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
 borderedText = new BorderedText(textSizePx);
 borderedText.setTypeface(Typeface.MONOSPACE);

 tracker = new MultiBoxTracker(this.getActivity());

 int cropSize = TF_OD_API_INPUT_SIZE;

 try {
 detector =
 FirebaseObjectDetectionAPIModel.create(
 getActivity().getAssets(),
 TF_OD_API_MODEL_FILE,
 TF_OD_API_LABELS_FILE,
 TF_OD_API_INPUT_SIZE,
 TF_OD_API_IS_QUANTIZED,
 getActivity().getApplicationContext());
 cropSize = TF_OD_API_INPUT_SIZE;
 } catch (final IOException e) {
 e.printStackTrace();
 LOGGER.e(e, "Exception initializing classifier!");
 Toast toast =
 Toast.makeText(
 getActivity().getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
 toast.show();
 getActivity().finish();
 }

 previewWidth = size.getWidth();
 previewHeight = size.getHeight();

 sensorOrientation = rotation - getScreenOrientation();
 LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

 //   LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
 rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
 croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

 frameToCropTransform =
 ImageUtils.getTransformationMatrix(
 previewWidth, previewHeight,
 cropSize, cropSize,
 sensorOrientation, MAINTAIN_ASPECT);

 cropToFrameTransform = new Matrix();
 frameToCropTransform.invert(cropToFrameTransform);

 trackingOverlay = (OverlayView) getView().findViewById(R.id.tracking_overlay);
 trackingOverlay.addCallback(
 new OverlayView.DrawCallback() {
@Override public void drawCallback(final Canvas canvas) {
tracker.draw(canvas);
}
});

 tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
 }
 */
/**
 protected int getLayoutId() {
 return R.layout.tfe_od_camera_connection_fragment_tracking;
 }

 */
/**
 protected Size getDesiredPreviewFrameSize() {
 return DESIRED_PREVIEW_SIZE;
 }
 */


}
