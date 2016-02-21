package org.matthelliwell.minecraftosloader.file;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import org.geotools.geometry.jts.JTS;

/**
 * Represents a contour loaded from shape file
 */
public class Contour {
    private double height;
    private MultiLineString multiLineString;


    public Contour() {
    }


    public void setHeight(final double height) {
        this.height = height;
    }

    public double getHeight() {
        return height;
    }

    public void setMultiLineString(final MultiLineString multiLineString) {
        this.multiLineString = multiLineString;
    }

    public Coordinate[] getSmoothedContour() {
        final Geometry smoothedLine = JTS.smooth(multiLineString, 0.0);
        final Geometry smoothedLine2 = JTS.smooth(smoothedLine, 0.0);
        final Coordinate[] coords = smoothedLine2.getCoordinates();
        for ( Coordinate coord: coords ) {
            coord.z = height;
        }

        return coords;
    }
}
