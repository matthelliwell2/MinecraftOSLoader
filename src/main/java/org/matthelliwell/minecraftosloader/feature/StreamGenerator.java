package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import org.matthelliwell.minecraftosloader.file.MultilineStringFileLoader;
import org.opengis.referencing.FactoryException;

public class StreamGenerator {
    private final FeatureGrid featureGrid;

    public StreamGenerator(final FeatureGrid featureGrid) {
        this.featureGrid = featureGrid;
    }

    public void generate(final Path path, final String gridSquare) throws IOException, FactoryException {
        final File file = path.resolve(gridSquare.toUpperCase() + "_SurfaceWater_Line.shp").toFile();
        new MultilineStringFileLoader(file, featureGrid.getBounds(), this::onNewStream).processFile();
    }

    private void onNewStream(final List<Coordinate> stream) {
        for ( final Coordinate c: stream ) {
            featureGrid.setFeature((int)c.x, (int)c.y, FeatureGrid.LAKE);
        }
    }
}
