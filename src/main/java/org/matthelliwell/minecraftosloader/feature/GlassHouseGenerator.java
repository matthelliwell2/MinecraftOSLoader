package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.vividsolutions.jts.geom.Point;
import org.matthelliwell.minecraftosloader.file.MultiploygonFileLoader;

public class GlassHouseGenerator {
    private final FeatureGrid featureGrid;

    public GlassHouseGenerator(final FeatureGrid featureGrid) {
        this.featureGrid = featureGrid;
    }
    public void generate(final Path path, final String gridSquare) throws IOException {
        final File file = path.resolve(gridSquare.toUpperCase() + "_Glasshouse.shp").toFile();

        // Not everywhere has glasshouses
        if ( file.exists() ) {
            new MultiploygonFileLoader(file, featureGrid.getBounds(), this::onNewPointInBuilding).processFile();
        }
    }

    private void onNewPointInBuilding(final Point p) {
        final int x = (int)p.getX();
        final int y = (int)p.getY();

        featureGrid.setFeature(x, y, FeatureGrid.GLASSHOUSE);
    }
}
