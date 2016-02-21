package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.vividsolutions.jts.geom.Point;
import org.matthelliwell.minecraftosloader.file.MultiploygonFileLoader;
import org.opengis.referencing.FactoryException;

/**
 * Loads the woodlands into the feature grid
 */
public class WoodlandGenerator {
    // Cumulative densities for different features
    private static final FeatureDensity[] FEATURE_DENSITIES = {
            new FeatureDensity(0.04, FeatureGrid.OAK),
            new FeatureDensity(0.06, FeatureGrid.BIRCH),
            new FeatureDensity(0.063, FeatureGrid.DANELION),
            new FeatureDensity(0.066, FeatureGrid.POPPY),
            new FeatureDensity(0.069, FeatureGrid.OXEYE_DAISY)
    };

    private final FeatureGrid featureGrid;

    public WoodlandGenerator(final FeatureGrid featureGrid) {
        this.featureGrid = featureGrid;

    }

    public void generate(final Path path, final String gridSquare) throws IOException, FactoryException {
        final File file = path.resolve(gridSquare.toUpperCase() + "_Woodland.shp").toFile();
        new MultiploygonFileLoader(file, featureGrid.getBounds(), this::onNewWoodland).processFile();
    }

    private void onNewWoodland(final Point p) {
        final int x = (int)p.getX();
        final int y = (int)p.getY();

        // We don't want tree at evert block so do some randomisation to try and make it look a bit pretty
        final double random = Math.random();
        for ( final FeatureDensity f: FEATURE_DENSITIES ) {
            if ( random < f.density ) {
                featureGrid.setFeature(x, y, f.feature);
                break;
            }
        }
    }

    private static class FeatureDensity {
        public FeatureDensity(final double density, final byte feature) {
            this.density = density;
            this.feature = feature;
        }

        private double density;
        private byte feature;
    }
}
