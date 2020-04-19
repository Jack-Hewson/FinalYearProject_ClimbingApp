package com.example.climbingapp.ui.Firebase;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.climbingapp.R;
import com.example.climbingapp.ui.env.Logger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public class FirebaseAPI {
    private static final Logger LOGGER = new Logger();
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    StorageReference xmlReference = storageReference.child("xml/");
    StorageReference imagesReference = storageReference.child("images/");
    StorageReference modelsReference = storageReference.child("tfliteModels");

    public void getStorageDownload() throws IOException {
        xmlReference.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        //for (StorageReference prefix: listResult.getPrefixes()) {

                        // }

                        for (StorageReference item : listResult.getItems()) {
                            LOGGER.i("STORAGE CONTENTS = " + item);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LOGGER.i("FAILURE AT FIREBASE_API");
                    }
                });

    }

    public void setFolder(Context context) throws IOException {
        String filename = "xmlFile";
        File localFile = File.createTempFile(filename, "xml");
        LOGGER.i("FILE LOCATION = " + localFile.getPath());
        xmlReference.child("20200219_095048_020.xml").getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        LOGGER.i("NEW TEMP FILE CREATED " +
                                taskSnapshot.getBytesTransferred() + "/" + taskSnapshot.getTotalByteCount());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        LOGGER.i("NEW TEMP FILE NOT CREATED FAILURE!");

                    }
                });
/**
 String filename = "myfile";
 String fileContents = "Hello world!";
 try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
 byte[] byteContents = fileContents.getBytes();
 fos.write(byteContents);
 } catch (FileNotFoundException e) {
 e.printStackTrace();
 } catch (IOException e) {
 e.printStackTrace();
 }*/
    }

    public void uploadImage(Uri filePath, Context context) {
        if (filePath != null) {
            // Code for showing progressDialog while uploading
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            // Defining the child of storageReference
            StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());

            // adding listeners on upload or failure of image
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {// Image uploaded successfully. Dismiss dialog
                            progressDialog.dismiss();
                            Toast.makeText(context, "Image Uploaded!!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {// Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast.makeText(context, "Failed " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        // Progress Listener for loading percentage on the dialog box
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()
                                    / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploaded " + (int) progress + "%");
                        }
                    });
        }
    }

    public void uploadFile(Context context, String fileLocation) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Uploading...");
        progressDialog.show();

        //Upload input stream to Firebase

        InputStream stream = null;
        try {
            stream = new FileInputStream(new File("/data/user/0/com.example.climbingapp/files/" + fileLocation));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        UploadTask uploadTask = (UploadTask) xmlReference.child("test_upload.xml").putStream(stream)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        LOGGER.i("TEXT FILE UPLOADED SUCCESS");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        LOGGER.i("TEXT FILE UPLOAD FAILED");
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()
                                / taskSnapshot.getTotalByteCount());
                        progressDialog.setMessage("Uploaded " + (int) progress + "%");
                    }
                });
    }
}
