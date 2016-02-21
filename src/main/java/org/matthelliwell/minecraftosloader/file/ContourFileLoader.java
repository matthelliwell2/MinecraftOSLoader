package org.matthelliwell.minecraftosloader.file;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import com.vividsolutions.jts.geom.MultiLineString;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Loads contours from the OS data. It recieves the raw featur data and parses it into a more useful format before
 * passing it back to the call
 */
public class ContourFileLoader extends FileLoader {
    private final Consumer<Contour> onNewContour;

    public ContourFileLoader(final File file,
                             final Consumer<Contour> onNewContour) throws IOException {
        super(file);
        setOnNewFeature(this::onNewFeature);
        this.onNewContour = onNewContour;
    }


    private void onNewFeature(final SimpleFeature feature) {

        try {
            onNewContour.accept(loadContour(feature));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private Contour loadContour(final SimpleFeature feature) throws IOException {
        final Contour contour = new Contour();

        final Collection<? extends Property> properties = feature.getValue();
        for ( Property property: properties ) {
            final Object propertyValue = property.getValue();

            if ( propertyValue instanceof MultiLineString) {
                final MultiLineString mls = (MultiLineString) property.getValue();
                contour.setMultiLineString(mls);
            } else if ( propertyValue instanceof Double ) {
                final double height = (Double)propertyValue;
                contour.setHeight(height);

            }
        }

        return contour;
    }
}
