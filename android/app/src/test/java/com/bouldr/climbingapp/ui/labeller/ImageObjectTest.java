package com.bouldr.climbingapp.ui.labeller;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImageObjectTest {
    ImageObject imageObject = ImageObject.getInstance();

    /**IMAGE TESTING*/
    @Test
    public void getInstance() {
        ImageObject imgObj = ImageObject.getInstance();
        assertEquals(imageObject, imgObj);
    }

    @Test
    public void getFilename() {
        String filename = "fileNameTest";
        imageObject.setFilename(filename);
        String objFilename = imageObject.getFilename();
        assertEquals(filename, objFilename);
    }

    @Test
    public void getImgWidth() {
        int imgWidth = 16;
        imageObject.setImgWidth(imgWidth);
        int objImgWidth = imageObject.getImgWidth();
        assertEquals(imgWidth, objImgWidth);
    }

    @Test
    public void getImgHeight() {
        int imgHeight = 20;
        imageObject.setImgHeight(imgHeight);
        int objImgHeight = imageObject.getImgHeight();
        assertEquals(imgHeight, objImgHeight);
    }

    @Test
    public void getImgDepth() {
        int imgDepth = 69;
        imageObject.setImgDepth(imgDepth);
        int objImgDepth = imageObject.getImgDepth();

        assertEquals(imgDepth, objImgDepth);
    }

    /**HOLD TESTING*/
    @Test
    public void getHolds() {
        int xMin = 1;
        int xMax = 5;
        int yMin = 8;
        int yMax = 23;
        String holdname = "testHoldname";
        ImageObject.Holds hold = imageObject.new Holds(xMin, yMin, xMax, yMax);
        assertEquals(imageObject.getHolds().get(0), hold);

        hold.setHoldName(holdname);
        String objHoldname = hold.getHoldname();
        assertEquals(holdname, objHoldname);
    }
}