package com.bouldr.climbingapp.ui.labeller;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class FileProcessorTest {

    FileProcessor fileProcessor = new FileProcessor();

    @Test
    public void getMaxImageSize() {
        int origHeight = 1920;
        int origWidth = 1080;
        int expHeight = 1100;
        int expWidth = 618;
        int[] maxVals = fileProcessor.getMaxImageSize(origHeight,origWidth);
        assertEquals(expHeight, maxVals[0]);
        assertEquals(expWidth, maxVals[1]);
    }

    @Test
    public void getScaleReduciton() {
        int origHeight = 1738;
        int origWidth = 720;
        int expHeight = 1100;
        int expWidth = 455;
        int[] maxVals = fileProcessor.getMaxImageSize(origHeight,origWidth);
        assertEquals(expHeight, maxVals[0]);
        assertEquals(expWidth, maxVals[1]);

        double scale = fileProcessor.getScaleReduction(maxVals, origHeight);
        assertEquals(1.58, scale, 0.1);
    }

}