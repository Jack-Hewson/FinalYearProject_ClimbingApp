package com.example.climbingapp.ui;

import java.util.ArrayList;

public class ImageObject {
    private static ImageObject imageObject = new ImageObject();

    private ImageObject() {
    }

    public static ImageObject getInstance() {
        return imageObject;
    }

    private String filename;
    private int imgWidth;
    private int imgHeight;
    private int imgDepth;

    private ArrayList<Holds> holds = new ArrayList<>();

    /**
     * public ImageObject(String filename, int imgWidth, int imgHeight, int imgDepth) {
     * this.filename = filename;
     * this.imgWidth = imgWidth;
     * this.imgHeight = imgHeight;
     * this.imgDepth = imgDepth;
     * }
     */

    public class Holds {
        private String holdName;
        private int holdXMin;
        private int holdYMin;
        private int holdXMax;
        private int holdYMax;

        public Holds(String holdName, int holdXMin, int holdYMin, int holdXMax, int holdYMax) {
            this.holdName = holdName;
            this.holdXMin = holdXMin;
            this.holdYMin = holdYMin;
            this.holdXMax = holdXMax;
            this.holdYMax = holdYMax;
            holds.add(this);
        }

        public String getHoldname() {
            return holdName;
        }

        public int getHoldXMin() {
            return holdXMin;
        }

        public int getHoldYMin() {
            return holdYMin;
        }

        public int getHoldXMax() {
            return holdXMax;
        }

        public int getHoldYMax() {
            return holdYMax;
        }
    }

    public String getFilename() {
        return filename;
    }

    public int getImgWidth() {
        return imgWidth;
    }

    public int getImgHeight() {
        return imgHeight;
    }

    public int getImgDepth() {
        return imgDepth;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setImgWidth(int imgWidth) {
        this.imgWidth = imgWidth;
    }

    public void setImgHeight(int imgHeight) {
        this.imgHeight = imgHeight;
    }

    public void setImgDepth(int imgDepth) {
        this.imgDepth = imgDepth;
    }

    public ArrayList<Holds> getHolds() {
        return holds;
    }
}
