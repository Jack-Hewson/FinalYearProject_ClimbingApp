package com.example.climbingapp.ui.send;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.climbingapp.FileProcessor;
import com.example.climbingapp.MainActivity;
import com.example.climbingapp.ui.Firebase.FirebaseAPI;

import com.example.climbingapp.R;
import com.example.climbingapp.ui.ImageObject;
import com.example.climbingapp.ui.env.Logger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;

public class SendFragment extends Fragment {
    private static final Logger LOGGER = new Logger();
    // views for button
    private Button btnSelect, btnUpload;
    // view for image view
    private ImageView imageView;
    // Uri indicates, where the image will be picked from
    private Uri filePath;
    // request code
    private final int PICK_IMAGE_REQUEST = 22;
    // instance for firebase storage and StorageReference
    //private SendViewModel sendViewModel;
    private FirebaseAPI firebaseAPI = new FirebaseAPI();
    private FileProcessor fileProcessor = new FileProcessor();
    ImageObject imageObject = new ImageObject();
    byte[] imageByteArray;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_send, container, false);
        // initialise views
        btnSelect = view.findViewById(R.id.btnChoose);
        btnUpload = view.findViewById(R.id.btnUpload);
        imageView = view.findViewById(R.id.imgView);

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
                String fileLocation = fileProcessor.createXMLFile(getContext(), imageObject, imageName);
                LOGGER.i("file location = " + fileLocation);
                //FileInputStream fis = fileProcessor.readFile(getContext(), filename);
                firebaseAPI.uploadFile(getContext(), fileLocation);
            }
        });

        return view;
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

    // Override onActivityResult method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // checking request code and result code. if request code is PICK_IMAGE_REQUEST and
        // resultCode is RESULT_OK then set image in the image view
        int[] imageResized;
        // Get the Uri of data
        filePath = data.getData();
        try {
            // Setting image on image view using Bitmap
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), filePath);
            imageResized = fileProcessor.getMaxImageSize(bitmap.getHeight(), bitmap.getWidth());
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, imageResized[1],imageResized[0],true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            imageByteArray = baos.toByteArray();
            LOGGER.i("Image WIDTH = " + resized.getWidth());
            LOGGER.i("Image HEIGHT = " + resized.getHeight());
            imageObject.setImgWidth(resized.getWidth());
            imageObject.setImgHeight(resized.getHeight());
            imageView.setImageBitmap(resized);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}