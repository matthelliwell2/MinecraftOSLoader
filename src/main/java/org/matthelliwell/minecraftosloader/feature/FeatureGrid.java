package org.matthelliwell.minecraftosloader.feature;

import org.geotools.geometry.jts.ReferencedEnvelope;

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

    private final byte[][] features;
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;

    public FeatureGrid(final ReferencedEnvelope bounds) {
        this.bounds = bounds;

        features = new byte[(int)(bounds.getWidth()) + 1][(int)(bounds.getHeight()) + 1];
        minX = (int)bounds.getMinX();
        maxX = (int)bounds.getMaxX();
        minY = (int)bounds.getMinY();
        maxY = (int)bounds.getMaxY();
    }

    public byte getFeature(final int x, final int y) {
        return features[x - minX][y - minY];
    }

    public byte getFeatureWithBoundsCheck(final int x, final int y) {
        if ( inBounds(x, y) ) {
            return features[x - minX][y - minY];
        } else {
            return GRASS;
        }
    }

    private boolean inBounds(final int x, final int y) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    public void setFeature(final int x, final int y, final byte feature) {
        final int xCoord = x - minX;
        final int yCoord = y - minY;

        if (xCoord < 0 || xCoord >= features.length || yCoord < 0 || yCoord >= features[0].length) {
            // We sometimes get some coords outside the range. Presumably this is due to the interpolation
            // returning some extra points. We just ignore them.
            return;
        }

        features[xCoord][yCoord] = feature;
    }

    public ReferencedEnvelope getBounds() {
        // TODO
//        return new ReferencedEnvelope(getMinX(), getMaxX(), getMinY(), getMaxY(), bounds.getCoordinateReferenceSystem());
        return bounds;
    }
}
