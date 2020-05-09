package com.bouldr.climbingapp;

import android.content.Context;

import com.bouldr.climbingapp.ui.labeller.ImageObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileProcessor {
    ImageObject imageObject = ImageObject.getInstance();

    public File createFolder(Context context, String foldername) {
        File file = new File(context.getFilesDir(), foldername);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public File createFile(Context context, File foldername, String filename) {
        File file = new File(foldername + "/" + filename);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public String createXMLFile(Context context, String filename) {
        /** String fileName = "Hello_file";
         String string = "jello world";
         FileOutputStream fos = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
         fos.write(string.getBytes());
         fos.close();
         */

        //ImageObject imageObject = new ImageObject("TestFilename", 100, 200, 300);


        ArrayList<ImageObject.Holds> holds = imageObject.getHolds();
        //ImageObject.Holds hold1 = holds.get(0);

        String foldername = "xml";
        String filelocation = foldername + "/" + filename;
        String folder = "test";
        String imgFilename = imageObject.getFilename();
        String pcFilePath = "C:\\Users\\jacks\\OneDrive - University of Plymouth\\" + filelocation;
        String imgWidth = String.valueOf(imageObject.getImgWidth());
        String imgHeight = String.valueOf(imageObject.getImgHeight());
        String imgDepth = String.valueOf(imageObject.getImgDepth());
        //String holdName = hold.getHoldname();
        //String holdXMin = String.valueOf(hold1.getHoldXMin());
        // String holdYMin = String.valueOf(hold1.getHoldYMin());
        //String holdXMax = String.valueOf(hold1.getHoldXMax());
        //String holdYMax = String.valueOf(hold1.getHoldYMax());

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
            for (ImageObject.Holds hold : holds) {
                BuffWriter.write("<object>");
                BuffWriter.newLine();
                BuffWriter.write("<name>" + hold.getHoldname() + "</name>");
                BuffWriter.newLine();
                BuffWriter.write("<pose>Unspecified</pose>");
                BuffWriter.newLine();
                BuffWriter.write("<trunacated>0</trunacated>");
                BuffWriter.newLine();
                BuffWriter.write("<difficult>0</difficult>");
                BuffWriter.newLine();
                BuffWriter.write("<bndbox>");
                BuffWriter.newLine();
                BuffWriter.write("<xmin>" + hold.getHoldXMin() + "</xmin>");
                BuffWriter.newLine();
                BuffWriter.write("<ymin>" + hold.getHoldYMin() + "</ymin>");
                BuffWriter.newLine();
                BuffWriter.write("<xmax>" + hold.getHoldXMax() + "</xmax>");
                BuffWriter.newLine();
                BuffWriter.write("<ymax>" + hold.getHoldYMax() + "</ymax>");
                BuffWriter.newLine();
                BuffWriter.write("</bndbox>");
                BuffWriter.newLine();
                BuffWriter.write("</object>");
            }
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
            fis = new FileInputStream(new File("/data/user/0/com.bouldr.climbingapp/files/" + filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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

        imageObject.setImgScale((double) inHeight / (double) outHeight);
        return new int[]{outHeight, outWidth};
    }

    public double getScaleReduction(int[] newSize, int oldH, int oldW) {
        int newH = newSize[0];
        int newW = newSize[1];
        return (double) oldH / (double) newH;
    }

    public String getLocalModel() {
        try {
            String dirPath = "/data/user/0/com.bouldr.climbingapp/files/fireBaseModels/";
            File dir = new File(dirPath);
            String[] files = dir.list();

            for (String aFile : files) {
            }
            return files[0].split("\\.")[0];
        } catch (Exception e) {
            return "1";
        }
    }

    public void deleteOldModel(String lastestModel) {
        String oldModel = getLocalModel();

        if (Integer.parseInt(lastestModel.split("\\.")[0]) != Integer.parseInt(oldModel)) {
            String dirPath = "/data/user/0/com.bouldr.climbingapp/files/fireBaseModels/" + oldModel + ".tflite";
            File old = new File(dirPath);
            try {
                old.delete();
            } catch (Exception e) {
            }
        }
    }
}
