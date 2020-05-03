package com.example.climbingapp.ui.image;

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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.climbingapp.FileProcessor;
import com.example.climbingapp.R;
import com.example.climbingapp.ui.Firebase.FirebaseAPI;
import com.example.climbingapp.ui.ImageObject;
import com.example.climbingapp.ui.env.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageFragment extends Fragment {
    private static final Logger LOGGER = new Logger();
    // views for button
    private Button btnSelect, btnUpload;
    // view for image view
    private ImageView imageView;
    // Uri indicates, where the image will be picked from
    private Uri filePath;
    private File imgFile;
    // request code
    private final int PICK_IMAGE_REQUEST = 22;
    // instance for firebase storage and StorageReference
    //private SendViewModel sendViewModel;
    private FirebaseAPI firebaseAPI = new FirebaseAPI();
    private FileProcessor fileProcessor = new FileProcessor();
    ImageObject imageObject = ImageObject.getInstance();
    byte[] imageByteArray;
    ViewStub stub;

    public ImageFragment(File imgFile) {
        this.imgFile = imgFile;
    }

    public ImageFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        // initialise views
        btnSelect = view.findViewById(R.id.btnChoose);
        btnUpload = view.findViewById(R.id.btnUpload);
        imageView = view.findViewById(R.id.imgView);
        stub = view.findViewById(R.id.boxStub);

        if (imgFile != null) {
            loadImage(imgFile);
            loadStub();
        }
        // on pressing btnSelect SelectImage() is called
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
                String imageName = firebaseAPI.uploadImage(getContext(), imageByteArray);
                imageObject.setFilename(imageName + ".jpg");
                LOGGER.i("object file name = " + imageObject.getFilename());
                String fileLocation = fileProcessor.createXMLFile(getContext(), imageName);
                LOGGER.i("file location = " + fileLocation);
                //FileInputStream fis = fileProcessor.readFile(getContext(), filename);
                firebaseAPI.uploadFile(getContext(), fileLocation);
            }
        });
        return view;
    }

    private Bitmap resizeImage(Bitmap bitmap) {
        int[] imageResized = fileProcessor.getMaxImageSize(bitmap.getHeight(), bitmap.getWidth());
        double scale = fileProcessor.getScaleReduction(imageResized, bitmap.getHeight(), bitmap.getWidth());
        LOGGER.i("Scale = " + scale);
        return Bitmap.createScaledBitmap(bitmap, imageResized[1], imageResized[0], true);
    }

    private void showImage(Bitmap bitmap) {
        Bitmap resized = resizeImage(bitmap);
        Bitmap rotatedBitmap = rotateImage(resized);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        imageByteArray = baos.toByteArray();
        imageObject.setImgWidth(rotatedBitmap.getWidth());
        imageObject.setImgHeight(rotatedBitmap.getHeight());
        imageView.setImageBitmap(rotatedBitmap);
    }

    private Bitmap rotateImage(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void loadImage(File imgFile){
        Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        showImage(bitmap);
    }

    private void loadImage(Uri filePath) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), filePath);
            showImage(bitmap);
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
        startActivityForResult(
                Intent.createChooser(intent, "Select Image from here..."), PICK_IMAGE_REQUEST);
    }

    private void loadStub() {
        if (stub != null) {
            stub.inflate();
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