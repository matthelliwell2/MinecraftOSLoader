package org.matthelliwell.minecraftosloader.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import com.vividsolutions.jts.geom.MultiLineString;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

/***
 * Reads in data from the various OS files that we can handle
 */
public class RoadFileLoader extends FileLoader {
    // Bounds in which are mapping. Data can cover larger area than the region we are mapping so we need to filter
    // out some of the data
    private final ReferencedEnvelope mapBounds;

    private final Consumer<Road> onNewRoad;

    public RoadFileLoader(final File file,
                          final ReferencedEnvelope mapBounds,
                          final Consumer<Road> onNewRoad) throws IOException {
        super(file);
        this.mapBounds = mapBounds;
        setOnNewFeature(this::onNewFeature);
        this.onNewRoad = onNewRoad;
    }


    /**
     * Extracts we point in a polygon from the feature. Features are stored as multiploygons but we only look at the first
     * polygon as a simplification
     */
    private void onNewFeature(final SimpleFeature feature) {
        try {
            final BoundingBox featureBounds = feature.getBounds().toBounds(mapBounds.getCoordinateReferenceSystem());
            if (!featureBounds.intersects(mapBounds)) {
                return;
            }

            final Road road = new Road();

            final Collection<? extends Property> properties = feature.getValue();
            for (final Property property : properties) {
                final Object propertyValue = property.getValue();
                if (propertyValue instanceof MultiLineString) {
                    road.setMultiLineString((MultiLineString) propertyValue);
                } else if ( property.getName().getLocalPart().equals("CLASSIFICA")) {
                    road.setClassification((String)propertyValue);
                }
            }

            onNewRoad.accept(road);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
