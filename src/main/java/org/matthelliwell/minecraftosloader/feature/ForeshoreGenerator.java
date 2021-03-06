package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.vividsolutions.jts.geom.Point;
import org.matthelliwell.minecraftosloader.file.MultiploygonFileLoader;

public class ForeshoreGenerator {
    private final FeatureGrid featureGrid;
    public ForeshoreGenerator(final FeatureGrid featureGrid) {
        this.featureGrid = featureGrid;
    }

    public void generate(final Path path, final String gridSquare) throws IOException {
        final File file = path.resolve(gridSquare.toUpperCase() + "_Foreshore.shp").toFile();

        // Not everywhere has seaside
        if ( file.exists() ) {
            new MultiploygonFileLoader(file, featureGrid.getBounds(), this::onNewPointInWater).processFile();
        }
    }

    private void onNewPointInWater(final Point p) {
        final int x = (int)p.getX();
        final int y = (int)p.getY();

        featureGrid.setFeature(x, y, FeatureGrid.FORESHORE);
    }
}
