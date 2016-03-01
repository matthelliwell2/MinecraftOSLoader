package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.vividsolutions.jts.geom.Point;
import org.matthelliwell.minecraftosloader.file.MultiploygonFileLoader;

/**
 * Loads the lakes/ponds etc into the feature grid
 */
public class LakeGenerator {

    private final FeatureGrid featureGrid;

    public LakeGenerator(final FeatureGrid featureGrid) {
        this.featureGrid = featureGrid;

    }

    public void generate(final Path path, final String gridSquare) throws IOException {
        final File file = path.resolve(gridSquare.toUpperCase() + "_SurfaceWater_Area.shp").toFile();
        if (file.exists()) {
            new MultiploygonFileLoader(file, featureGrid.getBounds(), this::onNewPointInLake).processFile();
        }
    }

    private void onNewPointInLake(final Point p) {
        final int x = (int)p.getX();
        final int y = (int)p.getY();

        featureGrid.setFeature(x, y, FeatureGrid.LAKE);
    }
}
