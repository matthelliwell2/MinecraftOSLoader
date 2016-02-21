package org.matthelliwell.minecraftosloader.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

/***
 * Reads in data from the various OS files that we can handle
 */
public class MultiploygonFileLoader extends FileLoader {

    private static final GeometryFactory GEOMETRY_FACTORY = JTSFactoryFinder.getGeometryFactory(null);

    // Bounds in which are mapping. Woodland data covers larger area than the region we are mapping so we need to filter
    // out some of the data
    private final ReferencedEnvelope mapBounds;

    private final Consumer<Point> onNewPointInPolygon;

    public MultiploygonFileLoader(final File file,
                                  final ReferencedEnvelope mapBounds,
                                  final Consumer<Point> onNewPointInPolygon) throws IOException {
        super(file);
        this.mapBounds = mapBounds;
        setOnNewFeature(this::onNewFeature);
        this.onNewPointInPolygon = onNewPointInPolygon;
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

            final Collection<? extends Property> properties = feature.getValue();
            for (final Property property : properties) {
                final Object propertyValue = property.getValue();
                if (propertyValue instanceof MultiPolygon) {
                    // Get the boundary of the wood, lake etc
                    final Geometry boundaryOfPolygon = ((MultiPolygon) propertyValue).getGeometryN(0);
                    final PreparedGeometry fastBoudary = PreparedGeometryFactory.prepare(boundaryOfPolygon);

                    // Now find all points inside the boundry
                    for (int x = (int) featureBounds.getMinX(); x < featureBounds.getMaxX(); ++x) {
                        for (int y = (int) featureBounds.getMinY(); y < featureBounds.getMaxY(); ++y) {
                            final Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
                            if (fastBoudary.contains(p)) {
                                onNewPointInPolygon.accept(p);
                            }
                        }
                    }
                }
            }
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
