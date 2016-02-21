package org.matthelliwell.minecraftosloader.file;

import com.vividsolutions.jts.geom.Point;

/**
 * Represents a contour loaded from shape file
 */
public class SpotHeight {
    private int height;
    private Point point;


    public SpotHeight() {
    }


    public void setPoint(final Point point) {
        this.point = point;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public double getX() {
        return point.getX();
    }

    public double getY() {
        return point.getY();
    }
}
