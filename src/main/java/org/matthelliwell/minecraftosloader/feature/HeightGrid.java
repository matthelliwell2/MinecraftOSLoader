package org.matthelliwell.minecraftosloader.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.vividsolutions.jts.geom.Coordinate;
import net.morbz.minecraft.world.Region;
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

    // Used to iterate through region in parallel. Note that the pool size must be < region cache size. This is becaus
    // we assume that regions don't get kicked out the cache during processing
    private static final int NUM_THREADS = 4;

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

    /**
     * Iterators over each cell in the grid. We iterate region by region, rather than row by row, to reduce the
     * amount of cache misses in the region cache.
     * @param eachCell Function to be called for each cell
     */
    public void forEachRegionInParallel(TriConsumer<Integer, Integer, Float> eachCell) {
        // Create a new exector for each call so that we can explicitly shut it down as it doesn't use daemon
        // threads by default, otherwise the program won't exit
        final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);

        // Coords of lower left corner of first region that in the area
        int xstart = (getMinX() / Region.BLOCKS_PER_REGION_SIDE) * Region.BLOCKS_PER_REGION_SIDE;
        int ystart = convertCoord(getMinY());
        ystart = (ystart / Region.BLOCKS_PER_REGION_SIDE) * Region.BLOCKS_PER_REGION_SIDE;
        ystart = convertCoord(ystart);

        int regionsInXDirection = Math.abs((getMaxX() - xstart) / Region.BLOCKS_PER_REGION_SIDE + 1);
        int regionsInYDirection = Math.abs((getMaxY() - ystart) / Region.BLOCKS_PER_REGION_SIDE + 1);

        // Note these loops will probably only work for positive coords
        // I hope these loops work and never have to be touched again as I don't like the look of them
        final List<Future> futures = new ArrayList<>(regionsInXDirection + regionsInYDirection);
        for ( int xregion = 0; xregion < regionsInXDirection; ++xregion) {
            for ( int yregion = 0; yregion < regionsInYDirection; ++yregion) {
                futures.add(executor.submit(new ForEachBlockInRegion(eachCell, xstart, ystart, xregion, yregion)));
            }
        }

        waitForFuturesToFinish(executor, futures);
    }

    private static void waitForFuturesToFinish(final ExecutorService executor, final List<Future> futures) {
        for ( Future f: futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
    }

    /**
     * Converts from a y coord on the grid to a z coord in minecraft and vica vera
     */
    private int convertCoord(int c) {
        final ReferencedEnvelope realBounds = getRealBounds();
        return (int) (-c + realBounds.getMinY() + realBounds.getMaxY());
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

    private class ForEachBlockInRegion implements Runnable {
        final TriConsumer<Integer, Integer, Float> eachCell;
        final int xstart;
        final int ystart;
        final int xregion;
        final int yregion;

        public ForEachBlockInRegion(final TriConsumer<Integer, Integer, Float> eachCell, final int xstart, final int ystart, final int xregion, final int yregion) {
            this.eachCell = eachCell;
            this.xstart = xstart;
            this.ystart = ystart;
            this.xregion = xregion;
            this.yregion = yregion;
        }

        @Override
        public void run() {
//            System.out.println("Iterating region x=" + xregion + ", y=" + yregion);
            for (int x = xstart + xregion * Region.BLOCKS_PER_REGION_SIDE, xcount = 0; x < getMaxX() && xcount < Region.BLOCKS_PER_REGION_SIDE; ++x, ++xcount ) {
                for ( int y = ystart + yregion * Region.BLOCKS_PER_REGION_SIDE, ycount = 0; y < getMaxY() && ycount < Region.BLOCKS_PER_REGION_SIDE; ++y, ++ycount ) {
                    // If we're iteratoring through a region that only partly intersects this grid then we need to skip
                    // over the coordinates that are outside the grid
                    if (x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY()) {
                        eachCell.accept(x, y, getHeight(x, y));
                    }
                }
            }
//            System.out.println("Finished iterating region x=" + xregion + ", y=" + yregion);
        }
    }
}
