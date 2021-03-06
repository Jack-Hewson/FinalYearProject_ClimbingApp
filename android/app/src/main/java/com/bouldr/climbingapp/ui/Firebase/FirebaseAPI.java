package com.bouldr.climbingapp.ui.Firebase;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bouldr.climbingapp.ui.labeller.FileProcessor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.UUID;

//API for managing interactions between Android and Firebase
public class FirebaseAPI {
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    StorageReference xmlReference = storageReference.child("xml/");
    StorageReference imagesReference = storageReference.child("images/");
    StorageReference modelsReference = storageReference.child("tfliteModels");
    FileProcessor fileProcessor = new FileProcessor();

    //method for uploading an image to firebase, a random name is generated for the image and the
    //provided bitmap is uploaded
    public String uploadImage(Context context, byte[] bitmap) {
        String filename = UUID.randomUUID().toString();
        if (bitmap != null) {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading image...");
            progressDialog.show();

            UploadTask uploadTask = (UploadTask) storageReference.child("images/" + filename + ".jpg")
                    .putBytes(bitmap)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {// Image uploaded successfully. Dismiss dialog
                            progressDialog.dismiss();
                            Toast.makeText(context, "Image Uploaded!",
                                    Toast.LENGTH_LONG).show();
                        }
                    })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {// Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast.makeText(context, "Failed " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
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
        return filename;
    }

    //NOT USED: uploads images to Firebase with the provided Uri
    public String uploadImage(Context context, Uri filePath) {
        String fileName = UUID.randomUUID().toString();

        if (filePath != null) {
            // Code for showing progressDialog while uploading
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            // Defining the child of storageReference
            StorageReference ref = storageReference.child("images/" + fileName + ".jpg");

            // adding listeners on upload or failure of image
            ref.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {// Image uploaded successfully. Dismiss dialog
                            progressDialog.dismiss();
                            Toast.makeText(context, "Image Uploaded!",
                                    Toast.LENGTH_LONG).show();
                        }
                    })

                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {// Error, Image not uploaded
                            progressDialog.dismiss();
                            Toast.makeText(context, "Failed " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
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

        return fileName;
    }

    //Uploads any file to Firebase given the String value for the local filelocation
    public void uploadFile(Context context, String fileLocation) {
        if (fileLocation != null) {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading XML file...");
            progressDialog.show();

            //Upload input stream to Firebase
            InputStream stream = null;
            try {
                stream = new FileInputStream(new File("/data/user/0/com.bouldr.climbingapp/files/" + fileLocation));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            UploadTask uploadTask = (UploadTask) storageReference.child(fileLocation + ".xml").putStream(stream)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss();
                            Toast.makeText(context, "Image Uploaded!",
                                    Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
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

    //Downloads the latest model from Firebase
    //Places the model in a folder for models (creates the folder if it doesn't exist)
    public void downloadModel(Context context, ProgressBar progressBar, Button btnDownload) {
        Toast.makeText(context, "Downloading model from the cloud", Toast.LENGTH_LONG).show();
        File folder = fileProcessor.createFolder(context, "fireBaseModels");
        btnDownload.setText("Downloading...");
        btnDownload.setClickable(false);

        getLatestCloudModel(new FirebaseCallback() {
            @Override
            public void onFirebaseCallback(StorageReference value) {
                String latestFilename = value.toString().split("/")[4];
                File file = fileProcessor.createFile(folder, latestFilename);
                modelsReference.child(latestFilename).getFile(file)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(context, "Download successful", Toast.LENGTH_LONG).show();
                                fileProcessor.deleteOldModel(latestFilename);
                                btnDownload.setBackgroundColor(Color.parseColor("#77dd77"));
                                btnDownload.setText("Downloaded");                            }

                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show();
                                btnDownload.setText("Failed");
                                btnDownload.setBackgroundColor(Color.parseColor("#ff6961"));
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(@NonNull FileDownloadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0 * taskSnapshot.getBytesTransferred()
                                        / taskSnapshot.getTotalByteCount());
                                progressBar.setProgress((int) progress);
                            }
                        });
            }
        });
    }

    //Callback for other classes waiting for a model to be checked on the cloud
    public interface FirebaseCallback {
        void onFirebaseCallback(StorageReference value);
    }

    //Retrieves the name of the latest model from Firebase. Used to compare with local model
    public void getLatestCloudModel(FirebaseCallback myCallback) {
        final StorageReference[] latestModel = {null};
        modelsReference.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference model : listResult.getItems()) {
                            String filename = model.toString().split("/")[4];
                            int fileVal = Integer.parseInt(filename.split("\\.")[0]);

                            if (latestModel[0] != null) {
                                String latestFilename = latestModel[0].toString().split("/")[4];
                                int latestFileVal = Integer.parseInt(latestFilename.split("\\.")[0]);

                                if (latestFileVal < fileVal) {
                                    latestModel[0] = model;
                                }
                            } else {
                                latestModel[0] = model;
                            }
                        }
                        myCallback.onFirebaseCallback(latestModel[0]);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                });
    }
}
