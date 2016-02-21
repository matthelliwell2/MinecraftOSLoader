package org.matthelliwell.minecraftosloader.file;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.operation.buffer.BufferParameters;
import org.opengis.geometry.BoundingBox;

public class Road {

    private MultiLineString multiLineString;
    private RoadClassification classification;

    public Road()
    }

    public void setMultiLineString(final MultiLineString multiLineString) {
        this.multiLineString = multiLineString;
    }

    public void setClassification(final String value) {
        classification = RoadClassification.fromValue(value);
    }


    /**
     * Returns a pair of representing the road of either side of the centre line. It is returned as two geometries as
     *  the BufferOp seems to return better looking results that way.
     */
    public Geometry getFullWidthRoad() {

        return multiLineString.buffer(classification.getWidth(), BufferParameters.DEFAULT_QUADRANT_SEGMENTS, BufferParameters.CAP_ROUND);
    }
}
