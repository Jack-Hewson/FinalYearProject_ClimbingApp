package com.bouldr.climbingapp.ui.image;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bouldr.climbingapp.R;
import com.bouldr.climbingapp.ui.Firebase.FirebaseAPI;
import com.bouldr.climbingapp.ui.env.Logger;
import com.bouldr.climbingapp.ui.labeller.FileProcessor;
import com.bouldr.climbingapp.ui.labeller.ImageObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

//Fragment that is the base for labelling images
public class ImageFragment extends androidx.fragment.app.Fragment {
    private static final Logger LOGGER = new Logger();
    private Button btnRotate;
    // view for image view
    private ImageView imageView;
    // Uri indicates, where the image will be picked from
    private File imgFile;
    // instance for firebase storage and StorageReference
    private FirebaseAPI firebaseAPI = new FirebaseAPI();
    private FileProcessor fileProcessor = new FileProcessor();
    private ImageObject imageObject = ImageObject.getInstance();
    private byte[] imageByteArray;
    private ViewStub stub;
    private Bitmap bitmap;
    private boolean isInflated;

    //Can be called from CameraFragment indicating a photograph has been taken
    public ImageFragment(File imgFile) {
        this.imgFile = imgFile;
    }

    //Loading from navigation bar indicates that no image has been selected yet
    public ImageFragment() {
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //If there is an image then load it and display
        if (imgFile != null) {
            loadImage(imgFile);
            loadStub();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        Button btnSelect = view.findViewById(R.id.btnChoose);
        Button btnUpload = view.findViewById(R.id.btnUpload);
        btnRotate = view.findViewById(R.id.btnRotate);
        imageView = view.findViewById(R.id.imgView);

        stub = view.findViewById(R.id.boxStub);
        stub.setOnInflateListener(new ViewStub.OnInflateListener() {
            @Override
            public void onInflate(ViewStub stub, View inflated) {
                isInflated = true;
            }
        });
        // on pressing btnSelect, SelectImage() is called
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectImage();
            }
        });

        // on pressing btnUpload uploadImage() is called
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imageLabelled()) {
                    String imageName = firebaseAPI.uploadImage(getContext(), imageByteArray);
                    imageObject.setFilename(imageName + ".jpg");
                    String fileLocation = fileProcessor.createXMLFile(getContext(), imageName);
                    //FileInputStream fis = fileProcessor.readFile(getContext(), filename);
                    firebaseAPI.uploadFile(getContext(), fileLocation);
                    stub.setVisibility(View.GONE);
                    imageView.setImageResource(0);
                } else {
                    Toast.makeText(getContext(), "Image not labelled",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        //Selecting this button will rotate the image by 90 degrees if there is an image and it
        //hasn't been labelled yet
        btnRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!imageLabelled()) {
                    try {
                        showImage();
                    } catch (Exception e) {

                    }
                }
            }
        });
        return view;
    }

    //Checks if the image has been labelled yet, returns true if it has
    public boolean imageLabelled() {
        if (imageObject.getHolds() == null || imageObject.getHolds().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    //Resizes the image so it is appropriate size for Firebase and model training
    private Bitmap resizeImage(Bitmap bitmap) {
        int[] imageResized = fileProcessor.getMaxImageSize(bitmap.getHeight(), bitmap.getWidth());
        LOGGER.i("ORIGINAL = " + bitmap.getHeight() + " " + bitmap.getWidth() + " NEW = " + imageResized[1] + " " + imageResized[0]);
        double scale = fileProcessor.getScaleReduction(imageResized, bitmap.getHeight());
        return Bitmap.createScaledBitmap(bitmap, imageResized[1], imageResized[0], true);
    }

    //Main function for loading the image onto the ImageView
    private void showImage() {
        Bitmap resized = resizeImage(bitmap);
        Bitmap rotatedBitmap = rotateImage(resized);
        this.bitmap = rotatedBitmap;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        imageByteArray = baos.toByteArray();
        imageObject.setImgWidth(rotatedBitmap.getWidth());
        imageObject.setImgHeight(rotatedBitmap.getHeight());
        imageView.setImageBitmap(rotatedBitmap);
    }

    //Called when rotate button is selected, rotates image by 90 degrees
    private Bitmap rotateImage(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        try {
            matrix.postRotate(90);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    }

    //Loads image by File
    private void loadImage(File imgFile) {
        bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        showImage();
    }

    //Loads image by Uri
    private void loadImage(Uri filePath) {
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), filePath);
            showImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Select Image method
    private void SelectImage() {
        // Defining Implicit Intent to mobile gallery
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // request code
        int PICK_IMAGE_REQUEST = 22;
        startActivityForResult(
                Intent.createChooser(intent, "Select Image from here..."), PICK_IMAGE_REQUEST);
    }

    //Loads the labeller stub
    private void loadStub() {
        if (stub != null && !isInflated) {
            stub.inflate();
        } else {
            stub.setVisibility(View.VISIBLE);
        }
    }

    // Override onActivityResult method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadImage(data.getData());
        loadStub();
    }
}