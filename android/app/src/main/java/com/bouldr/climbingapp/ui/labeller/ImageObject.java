package com.bouldr.climbingapp.ui.labeller;

import java.util.ArrayList;

//Contains the information about the current image that is being labelled and the label information
public class ImageObject {
    private String filename;
    private int imgWidth;
    private int imgHeight;
    private int imgDepth;
    private double imgScale;

    private static ImageObject imageObject = new ImageObject();

    private ImageObject() {}

    public static ImageObject getInstance() {
        return imageObject;
    }

    private ArrayList<Holds> holds = new ArrayList<>();
    
    public class Holds {
        private String holdName;
        private int holdXMin;
        private int holdYMin;
        private int holdXMax;
        private int holdYMax;

        //Scaling required because of offset on screen
        public Holds(int holdXMin, int holdYMin, int holdXMax, int holdYMax) {
            this.holdXMin = (int) Math.floor(holdXMin / (imgScale * 0.75));
            this.holdYMin = (int) Math.floor(holdYMin / (imgScale * 0.75));
            this.holdXMax = (int) Math.ceil(holdXMax / (imgScale * 0.75));
            this.holdYMax = (int) Math.ceil(holdYMax / (imgScale * 0.75));
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

        public void setHoldName(String holdName){
            this.holdName = holdName;
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

    public void setImgScale(double imgScale) {
        this.imgScale = imgScale;
    }

    public double getImgScale() {
        return imgScale;
    }

    public ArrayList<Holds> getHolds() {
        return holds;
    }

    public void deleteHolds() {
        holds.clear();
    }
}
