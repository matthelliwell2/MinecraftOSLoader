package org.matthelliwell.minecraftosloader.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiLineString;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

public class MultilineStringFileLoader extends FileLoader {

    // Bounds in which are mapping. Woodland data covers larger area than the region we are mapping so we need to filter
    // out some of the data
    private final ReferencedEnvelope mapBounds;

    private final Consumer<List<Coordinate>> onNewLine;

    public MultilineStringFileLoader(final File file,
                                  final ReferencedEnvelope mapBounds,
                                  final Consumer<List<Coordinate>> onNewNewLine) throws IOException {
        super(file);
        this.mapBounds = mapBounds;
        setOnNewFeature(this::onNewFeature);
        this.onNewLine = onNewNewLine;
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
                if (propertyValue instanceof MultiLineString) {
                    onNewMultilineString((MultiLineString) propertyValue);
                }
            }
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a smooth list of coords between each point on the multiline string, with no gaps and no diagonals
     */
    private void onNewMultilineString(final MultiLineString multiLineString) {
        final List<Coordinate> results = new ArrayList<>();
        final Coordinate[] coords = JTS.smooth(multiLineString, 0.0).getCoordinates();
        for (int i = 0; i < coords.length - 1; ++i) {
            final Coordinate first = coords[i];
            final Coordinate second = coords[i + 1];

            results.addAll(lineBetweenPoints(first, second));
        }

        onNewLine.accept(results);
    }

    private static List<Coordinate> lineBetweenPoints(final Coordinate first, final Coordinate second) {
        // Make sure x is increasing as it makes the looping a bit easier
        final Coordinate left;
        final Coordinate right;
        if ( first.x > second.x ) {
            left = second;
            right = first;
        } else {
            left = first;
            right = second;
        }

        final int xrange = (int) right.x - (int) left.x;
        final int yrange = (int) right.y - (int) left.y;

        if ( xrange == 0 ) {
            return lineBetweenVerticalPoints(left, right);
        }

        final List<Coordinate> results = new ArrayList<>(Math.abs(xrange) + Math.abs(yrange));
        final double slope = (double)yrange / xrange;

        int prevX = Integer.MAX_VALUE;
        int prevY = Integer.MAX_VALUE;
        boolean firstCoord = true;
        double stepSize = (double)xrange / (xrange + Math.abs(yrange));
        for ( double x = left.x; (int)x <= (int)right.x; x += stepSize) {
            final int newY  = (int)(left.y + slope * (x - left.x));
            final int newX = (int)x;
            if ( firstCoord ) {
                results.add(new Coordinate(newX, newY));
                prevX = newX;
                prevY = newY;
                firstCoord = false;
                if ( newX == 277095 && newY == 378593 ) {
                    System.out.println("Xxxx");
                }
            } else if (prevX != newX || prevY != newY) {
                // We've moved to a new block so we need to add it to the list of coords.
                // If we've moved diagonaly make sure we add an intermediate block as we want a continuous line
                if ( prevX != newX && prevY != newY ) {
                    results.add(new Coordinate(prevX, newY));
                }

                if ( newX == 277095 && newY == 378593 ) {
                    System.out.println("Xxxx");
                }
                results.add(new Coordinate(newX, newY));
                prevX = newX;
                prevY = newY;
            }
        }

        return results;
    }

    private static List<Coordinate> lineBetweenVerticalPoints(final Coordinate first, final Coordinate second) {
        // Make sure y is increasing as it makes the looping a bit easier
        final Coordinate bottom;
        final Coordinate top;
        if ( first.y > second.y ) {
            top = first;
            bottom = second;
        } else {
            top = second;
            bottom = first;
        }


        final List<Coordinate> results = new ArrayList<>();
        for ( int y = (int)bottom.y; y <= top.y; ++y ) {
            results.add(new Coordinate(first.x, y));
        }

        return results;
    }

}
