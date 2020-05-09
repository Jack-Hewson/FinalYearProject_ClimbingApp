package com.bouldr.climbingapp.ui.Firebase;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bouldr.climbingapp.FileProcessor;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class FirebaseAPI {
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();
    StorageReference xmlReference = storageReference.child("xml/");
    StorageReference imagesReference = storageReference.child("images/");
    StorageReference modelsReference = storageReference.child("tfliteModels");
    FileProcessor fileProcessor = new FileProcessor();

    public void setFolder(Context context) throws IOException {
        String filename = "xmlFile";
        File localFile = File.createTempFile(filename, "xml");
        xmlReference.child("20200219_095048_020.xml").getFile(localFile)
                .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
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

    public String uploadImage(Context context, byte[] bitmap) {
        String filename = UUID.randomUUID().toString();
        if (bitmap != null) {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            UploadTask uploadTask = (UploadTask) storageReference.child("images/" + filename + ".jpg")
                    .putBytes(bitmap)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {// Image uploaded successfully. Dismiss dialog
                            progressDialog.dismiss();
                            Toast.makeText(context, "Image Uploaded!",
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
        return filename;
    }

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

        return fileName;
    }

    public void uploadFile(Context context, String fileLocation) {
        if (fileLocation != null) {
            ProgressDialog progressDialog = new ProgressDialog(context);
            progressDialog.setTitle("Uploading...");
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

    public void downloadModel(Context context, ProgressBar progressBar) {
        Toast.makeText(context, "Downloading model from the cloud", Toast.LENGTH_SHORT).show();
        File folder = fileProcessor.createFolder(context, "fireBaseModels");
        getLatestCloudModel(new FirebaseCallback() {
            @Override
            public void onFirebaseCallback(StorageReference value) {
                String latestFilename = value.toString().split("/")[4];
                File file = fileProcessor.createFile(context, folder, latestFilename);
                modelsReference.child(latestFilename).getFile(file)
                        .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                Toast.makeText(context, "Download successful", Toast.LENGTH_SHORT).show();
                                fileProcessor.deleteOldModel(latestFilename);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show();
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

    public interface FirebaseCallback {
        void onFirebaseCallback(StorageReference value);
    }

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
