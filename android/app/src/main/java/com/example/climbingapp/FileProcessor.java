package com.example.climbingapp;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import com.example.climbingapp.ui.ImageObject;
import com.example.climbingapp.ui.env.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class FileProcessor {
    private static final Logger LOGGER = new Logger();

    public File createFolder(Context context, String foldername) {
        File file = new File(context.getFilesDir(), foldername);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public File createFile(Context context, File foldername, String filename) {
        File file = new File(foldername + "/" + filename);
        LOGGER.i("Creating File " + file.getAbsolutePath());
        if (!file.exists()) {
            try {
                file.createNewFile();
                LOGGER.i("File created");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LOGGER.i("File already exists");
        }
        return file;
    }

    public String createXMLFile(Context context, ImageObject imageObject, String filename) {
        /** String fileName = "Hello_file";
         String string = "jello world";
         FileOutputStream fos = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
         fos.write(string.getBytes());
         fos.close();
         */

        //ImageObject imageObject = new ImageObject("TestFilename", 100, 200, 300);
        ImageObject.Holds hold = imageObject.new Holds("HOLD1", 10, 20, 30, 40);

        String foldername = "xml";
        String filelocation = foldername + "/" + filename;
        String folder = "test";
        String imgFilename = imageObject.getFilename();
        String pcFilePath = "C:\\Users\\jacks\\OneDrive - University of Plymouth\\" + filelocation;
        String imgWidth = String.valueOf(imageObject.getImgWidth());
        String imgHeight = String.valueOf(imageObject.getImgHeight());
        String imgDepth = String.valueOf(imageObject.getImgDepth());
        String holdName = hold.getHoldname();
        String holdXMin = String.valueOf(hold.getHoldXMin());
        String holdYMin = String.valueOf(hold.getHoldYMin());
        String holdXMax = String.valueOf(hold.getHoldXMax());
        String holdYMax = String.valueOf(hold.getHoldYMax());

        File file = createFolder(context, foldername);

        try {
            File gpxfile = new File(file, filename);
            FileWriter writer = new FileWriter(gpxfile);
            BufferedWriter BuffWriter = new BufferedWriter(writer);
            BuffWriter.write("<annotation>");
            BuffWriter.newLine();
            BuffWriter.write("<folder>" + folder + "</folder>"); //Folder = train or test
            BuffWriter.newLine();
            BuffWriter.write("<filename>" + imgFilename + "</filename>"); //filename ends in jpg
            BuffWriter.newLine();
            BuffWriter.write("<path>" + pcFilePath + "</path>");
            BuffWriter.newLine();
            BuffWriter.write("<source>");
            BuffWriter.newLine();
            BuffWriter.write("<database>Unknown</database>");
            BuffWriter.newLine();
            BuffWriter.write("</source>");
            BuffWriter.newLine();
            BuffWriter.write("<size>");
            BuffWriter.newLine();
            BuffWriter.write("<width>" + imgWidth + "</width>");
            BuffWriter.newLine();
            BuffWriter.write("<height>" + imgHeight + "</height>");
            BuffWriter.newLine();
            BuffWriter.write("<depth>" + imgDepth + "</depth>");
            BuffWriter.newLine();
            BuffWriter.write("</size>");
            BuffWriter.newLine();
            BuffWriter.write("<segmented>0</segmented>");
            BuffWriter.newLine();
            //For Loop goes here
            BuffWriter.write("<object>");
            BuffWriter.newLine();
            BuffWriter.write("<name>" + holdName + "</name>");
            BuffWriter.newLine();
            BuffWriter.write("<pose>Unspecified</pose>");
            BuffWriter.newLine();
            BuffWriter.write("<trunacated>0</trunacated>");
            BuffWriter.newLine();
            BuffWriter.write("<difficult>0</difficult>");
            BuffWriter.newLine();
            BuffWriter.write("<bndbox>");
            BuffWriter.newLine();
            BuffWriter.write("<xmin>" + holdXMin + "</xmin>");
            BuffWriter.newLine();
            BuffWriter.write("<ymin>" + holdYMin + "</ymin>");
            BuffWriter.newLine();
            BuffWriter.write("<xmax>" + holdXMax + "</xmax>");
            BuffWriter.newLine();
            BuffWriter.write("<ymax>" + holdYMax + "</ymax>");
            BuffWriter.newLine();
            BuffWriter.write("</bndbox>");
            BuffWriter.newLine();
            BuffWriter.write("</object>");
            //For Loop ends here
            BuffWriter.newLine();
            BuffWriter.write("</annotation>");
            // writer.flush();
            BuffWriter.close();
        } catch (IOException e) {
        }

        return filelocation;
    }

    public FileInputStream readFile(Context context, String filename) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File("/data/user/0/com.example.climbingapp/files/" + filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
        return fis;
    }

    public int[] getMaxImageSize(int inHeight, int inWidth) {
        int outHeight;
        int outWidth;
        int maxSize = 1100;

        if (inWidth > inHeight) {
            outWidth = maxSize;
            outHeight = (inHeight * maxSize) / inWidth;
        } else {
            outHeight = maxSize;
            outWidth = (inWidth * maxSize) / inHeight;
        }
        return new int[] {outHeight, outWidth};
    }

    public String getLocalModel(){
        String dirPath = "/data/user/0/com.example.climbingapp/files/fireBaseModels/";
        File dir = new File(dirPath);
        String[] files = dir.list();

        for (String aFile: files) {
            LOGGER.i("afile " + aFile);
        }

        String modelVersion = files[0].split("\\.")[0];

        return modelVersion;
    }

    public void deleteOldModel(String file) {
        String dirPath = "/data/user/0/com.example.climbingapp/files/fireBaseModels/" + file + ".tflite";
        File old = new File(dirPath);

        LOGGER.i("Deleting old model " + dirPath);
        try {
            old.delete();
            LOGGER.i("Old model successfully deleted");
        } catch (Exception e) {
            LOGGER.i("ERROR: Cannot delete old model " + dirPath);
        }

    }
}
