package org.matthelliwell.minecraftosloader.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import com.vividsolutions.jts.geom.Point;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

/***
 * Reads in data from the various OS files that we can handle
 */
public class SpotHeightFileLoader extends FileLoader {
    private final Consumer<SpotHeight> onNewSpotHeight;

    public SpotHeightFileLoader(final File file,
                                final Consumer<SpotHeight> onNewSpotHeight) throws IOException {
        super(file);
        setOnNewFeature(this::onNewFeature);
        this.onNewSpotHeight = onNewSpotHeight;
    }


    private void onNewFeature(final SimpleFeature feature) {
        onNewSpotHeight.accept(loadSpotHeight(feature));
    }

    private SpotHeight loadSpotHeight(final SimpleFeature feature) {
        final SpotHeight spotHeight = new SpotHeight();

        final Collection<? extends Property> properties = feature.getValue();
        for ( Property property: properties ) {
            final Object propertyValue = property.getValue();

            if ( propertyValue instanceof Point) {
                final Point point = (Point) property.getValue();
                spotHeight.setPoint(point);
            } else if ( propertyValue instanceof Integer ) {
                final int height = (Integer)propertyValue;
                spotHeight.setHeight(height);
            }
        }

        return spotHeight;
    }
}
