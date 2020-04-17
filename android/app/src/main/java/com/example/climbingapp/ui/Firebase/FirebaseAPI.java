package com.example.climbingapp.ui.Firebase;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.example.climbingapp.ui.env.Logger;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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

                        for (StorageReference item: listResult.getItems()) {
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

    public void getFolder(Context context) throws FileNotFoundException {
        String filename = "myfile";
        FileInputStream fis = context.openFileInput(filename);
        LOGGER.i("GETFOLDER: " + fis.getChannel());
        InputStreamReader inputStreamReader =
                new InputStreamReader(fis, StandardCharsets.UTF_8);
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line).append('\n');
                line = reader.readLine();
            }
        } catch (IOException e) {
            // Error occurred when opening raw file for reading.
        } finally {
            String contents = stringBuilder.toString();
            LOGGER.i("FILE CONTENTS = " + contents);
        }
    }

    public void uploadImage() {
        Uri file = Uri.fromFile(new File("/Internal storage/DCIM/Camera/20200416_220021.jpg"));
        imagesReference.putFile(file)
                .addOnSuccessListener(
                        new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                LOGGER.i("IMAGE UPLOADED SUCCESSFULLY");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                LOGGER.i("IMAGE NOT UPLOADED");
                            }
                        });
    }
}
