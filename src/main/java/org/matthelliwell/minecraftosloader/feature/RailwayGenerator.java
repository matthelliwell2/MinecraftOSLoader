package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import org.matthelliwell.minecraftosloader.file.MultilineStringFileLoader;

public class RailwayGenerator {
    private final FeatureGrid featureGrid;

    public RailwayGenerator(final FeatureGrid featureGrid) {
        this.featureGrid = featureGrid;
    }

    public void generate(final Path path, final String gridSquare) throws IOException {
        final File file = path.resolve(gridSquare.toUpperCase() + "_RailwayTrack.shp").toFile();
        new MultilineStringFileLoader(file, featureGrid.getBounds(), this::onNewRailway).processFile();
    }

    private void onNewRailway(final List<Coordinate> stream) {
        for ( final Coordinate c: stream ) {
            featureGrid.setFeature((int)c.x, (int)c.y, FeatureGrid.RAILWAY);
        }
    }
}
