package com.bouldr.climbingapp.ui.labeller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

//ColorBall for all 4 balls (one of each corner)
class ColorBall {
    private Bitmap bitmap;
    private Point point;
    private int id;

    ColorBall(Context context, int resourceId, Point point, int id) {
        this.id = id;
        bitmap = BitmapFactory.decodeResource(context.getResources(),
                resourceId);
        this.point = point;
    }

    int getWidthOfBall() {
        return bitmap.getWidth();
    }

    int getHeightOfBall() {
        return bitmap.getHeight();
    }

    Bitmap getBitmap() {
        return bitmap;
    }

    int getX() {
        return point.x;
    }

    int getY() {
        return point.y;
    }

    int getID() {
        return id;
    }

    void setX(int x) {
        point.x = x;
    }

    void setY(int y) {
        point.y = y;
    }

    void addY(int y) {
        point.y = point.y + y;
    }

    void addX(int x) {
        point.x = point.x + x;
    }
}
