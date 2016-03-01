package org.matthelliwell.minecraftosloader.feature;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matthelliwell.util.ByteArray2D;

/**
 * Contains an indication of what is in each cell so that the writer can generate the appropriate blocks. This
 * is pretty basic but works so long a we don't want more than 255 different choices. Its up to the generators to
 * decide how this translates into minecraft blocks though it is usually one to one
 */
public class FeatureGrid {
    final public static byte GRASS = 0;
    final public static byte OAK = 1;
    final public static byte BIRCH = 2;
    final public static byte DANELION = 3;
    final public static byte POPPY = 4;
    final public static byte OXEYE_DAISY = 5;
    final public static byte LAKE = 6;
    final public static byte FUNCTIONAL_SITE = 7;
    final public static byte BUILDING = 8;
    final public static byte IMPORTANT_BUILDING = 9;
    final public static byte ROAD = 10;
    final public static byte GLASSHOUSE = 11;
    final public static byte TIDAL_WATER = 12;
    final public static byte FORESHORE = 13;
    final public static byte RAILWAY = 14;

    final private ReferencedEnvelope bounds;

    private final ByteArray2D features;

    public FeatureGrid(final ReferencedEnvelope bounds) {
        // Create a new bounds so it has the same rounding as we are using in are max/min calculations
        this.bounds = new ReferencedEnvelope((int)bounds.getMinX(),
                (int)bounds.getMaxX(),
                (int)bounds.getMinY(),
                (int)bounds.getMaxY(),
                bounds.getCoordinateReferenceSystem());
        features = new ByteArray2D(getMaxX() - getMinX() + 1, getMaxY() - getMinY() + 1, getMinX(), getMinY());
    }

    public byte getFeature(final int x, final int y) {
        return features.get(x, y);
    }

    public byte getFeatureWithBoundsCheck(final int x, final int y) {
        if ( withinBounds(x, y) ) {
            return getFeature(x, y);
        } else {
            return GRASS;
        }
    }

    private boolean withinBounds(final int x, final int y) {
        return bounds.contains(new Coordinate(x, y));
    }

    public void setFeature(final int x, final int y, final byte feature) {
        // We sometimes get some coords outside the range. Presumably this is due to the interpolation
        // returning some extra points. We just ignore them.
        if (withinBounds(x, y)) {
            features.set(x, y, feature);
        }
    }

    public ReferencedEnvelope getBounds() {
        // TODO
//        return new ReferencedEnvelope(getMinX(), getMaxX(), getMinY(), getMaxY(), bounds.getCoordinateReferenceSystem());
        return bounds;
    }

    private int getMinX() {
        return (int)bounds.getMinX();
    }

    private int getMaxX() {
        return (int)bounds.getMaxX();
    }

    private int getMinY() {
        return (int)bounds.getMinY();
    }

    private int getMaxY() {
        return (int)bounds.getMaxY();
    }

}
