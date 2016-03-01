package org.matthelliwell.minecraftosloader.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.vividsolutions.jts.geom.Coordinate;
import net.morbz.minecraft.world.Region;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matthelliwell.minecraftosloader.writer.CoordConverter;
import org.matthelliwell.util.FloatArray2D;
import org.matthelliwell.util.TriConsumer;

/**
 * A grid of the heights for each coordinate. Uses offsets to address the array so can stick to the native coordinates
 * in the shape files.
 */
public class HeightGrid {

    private final FloatArray2D heights;
    private float minHeight = Integer.MAX_VALUE;
    private float maxHeight = Integer.MIN_VALUE;
    final private ReferencedEnvelope bounds;

    // Used to iterate through region in parallel. Note that the pool size must be < region cache size. This is becaus
    // we assume that regions don't get kicked out the cache during processing
    private static final int NUM_THREADS = 4;

    public HeightGrid(final ReferencedEnvelope bounds) {
        // Create a new bounds so it has the same rounding as we are using in are max/min calculations
        this.bounds = new ReferencedEnvelope((int)bounds.getMinX(),
                (int)bounds.getMaxX(),
                (int)bounds.getMinY(),
                (int)bounds.getMaxY(),
                bounds.getCoordinateReferenceSystem());
        heights = new FloatArray2D(getMaxX() - getMinX() + 1, getMaxY() - getMinY() + 1, getMinX(), getMinY());

        // Sset the heights to some default value so we can tell if they've been set or not. Don't use zero
        // as zero (and small negative numbers) are valid heights
        heights.forEach((x, y, v) -> heights.set(x, y, Integer.MIN_VALUE));
    }

    public void forEach(TriConsumer<Integer, Integer, Float> eachCell) {
        heights.forEach(eachCell);
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

        // Get the x coords of the lower left corner of first region that in the area.
        int xstart = (getMinX() / Region.BLOCKS_PER_REGION_SIDE) * Region.BLOCKS_PER_REGION_SIDE;

        // Now do same for y coord but as north/south is inverted in MC we need to look at the max y coord
        // This will give is the max y coord in MC of this area
        int ystart = CoordConverter.convert(getMinY());
        // Calculate the high y/z coord in this region
        ystart = (ystart / Region.BLOCKS_PER_REGION_SIDE) * Region.BLOCKS_PER_REGION_SIDE + Region.BLOCKS_PER_REGION_SIDE - 1;
        // convert this back to a map y coord.
        ystart = CoordConverter.convert(ystart);

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


    public void setHeight(final double x, final double y, final double height) {
        // We sometimes get some coords outside the range. Presumably this is due to the interpolation
        // returning some extra points. We just ignore them.
        if (withinBounds((int)x, (int)y)) {
            heights.set((int)x, (int)y, (float)height);

            minHeight = (float) Math.min(minHeight, height);
            maxHeight = (float) Math.max(maxHeight, height);
        }
    }

    public boolean withinBounds(final int x, final int y) {
        return bounds.contains(new Coordinate(x, y));
    }

    public float getHeight(final int x, final int y) {
        return heights.get(x, y);
    }

    public float getHeightWithBoundsCheck(final int x, final int y) {
        if ( withinBounds(x, y)) {
            return getHeight(x, y);
        } else {
            return Integer.MIN_VALUE;
        }
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

    public ReferencedEnvelope getBounds() {
        return bounds;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public float getMaxHeight() {
        return maxHeight;
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
