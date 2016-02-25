package org.matthelliwell.minecraftosloader.feature;

import com.vividsolutions.jts.geom.Coordinate;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matthelliwell.minecraftosloader.TriConsumer;

/**
 * A grid of the heights for each coordinate. Uses offsets to address the array so can stick to the native coordinates
 * in the shape files.
 */
public class HeightGrid {
    private final float[][] heights;
    private float minHeight = Integer.MAX_VALUE;
    private float maxHeight = Integer.MIN_VALUE;
    final private ReferencedEnvelope bounds;
    private final int minX;
    private final int minY;

    public HeightGrid(final ReferencedEnvelope bounds) {
        this.bounds = bounds;
        minX = (int)bounds.getMinX();
        minY = (int)bounds.getMinY();
        heights = new float[(int)(bounds.getWidth()) + 1][(int)(bounds.getHeight()) + 1];

        // Sset the heights to some default value so we can tell if they've been set or not. Don't use zero
        // as zero (and small negative numbers) are valid heights
        for ( int x = 0; x < heights.length; ++x ) {
            for ( int y = 0; y < heights[x].length; ++y ) {
                heights[x][y] = Integer.MIN_VALUE;
            }
        }
    }

    public void forEach(TriConsumer<Integer, Integer, Float> eachCell) {
        for ( int x = getMinX(); x <= getMaxX(); ++x ) {
            for (int y = getMinY(); y <= getMaxY(); ++y) {
                eachCell.accept(x, y, getHeight(x, y));
            }
        }
    }

    public void setHeight(final double x, final double y, final double height) {

        final int xCoord = (int)(x - minX);
        final int yCoord = (int)(y - minY);

        if ( xCoord < 0 || xCoord >= heights.length || yCoord < 0 || yCoord >= heights[0].length ) {
            // We sometimes get some coords outside the range. Presumably this is due to the interpolation
            // returning some extra points. We just ignore them.
            return;
        }

        heights[xCoord][yCoord] = (int) Math.round(height);

        minHeight = (float) Math.min(minHeight, height);
        maxHeight = (float) Math.max(maxHeight, height);
    }

    public boolean withinBounds(final int x, final int y) {
        return bounds.contains(new Coordinate(x, y));
    }

    public float getHeight(final int x, final int y) {
        return heights[x - minX][y - minY];
    }

    public float getHeightWithBounds(final int x, final int y) {
        final int xCoord = x - minX;
        final int yCoord = y - minY;

        if ( xCoord < 0 || xCoord >= heights.length || yCoord < 0 || yCoord >= heights[0].length ) {
            return 0;
        } else {
            return heights[xCoord][yCoord];
        }
    }

    public int getMinX() {
//        return 378965;
        return (int)bounds.getMinX();
    }

    public int getMaxX() {
//        return 379798;
        return (int)bounds.getMaxX();
    }

    public int getMinY() {
//        return 355288;
        return (int)bounds.getMinY();
    }

    public int getMaxY() {
//        return 355945;
        return (int)bounds.getMaxY();
    }

    public ReferencedEnvelope getBounds() {
        // We create a new bounds here so we can manually set the min/max x and y if we want to test with a smaller area
        return new ReferencedEnvelope(getMinX(), getMaxX(), getMinY(), getMaxY(), bounds.getCoordinateReferenceSystem());
    }


    public ReferencedEnvelope getRealBounds() {
        return new ReferencedEnvelope(getMinX(), getMaxX(), getMinY(), getMaxY(), bounds.getCoordinateReferenceSystem());
    }

    public float getMinHeight() {
        return minHeight;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public int getNumCells() {
        return heights.length * heights[0].length;
    }
}
