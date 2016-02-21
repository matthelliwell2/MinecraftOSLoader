package org.matthelliwell.minecraftosloader.file;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Class for loading an OS file. Iterators through the features in a file and calls a function on each feature
 * it finds.
 */
public class FileLoader {
    // File we are loading
    private final File inputFile;
    // Feture source in the file
    private final SimpleFeatureSource featureSource;
    // Callback for each new feature
    private Consumer<SimpleFeature> onNewFeature;


    FileLoader(final File file) throws IOException {
        this.inputFile = file;
        final FileDataStore store = FileDataStoreFinder.getDataStore(file);
        this.featureSource = store.getFeatureSource();
    }

    void setOnNewFeature(final Consumer<SimpleFeature> onNewFeature) {
        this.onNewFeature = onNewFeature;
    }

    public void processFile() {
        try {
            System.out.println("Processing file " + inputFile.getName());

            try (SimpleFeatureIterator featureIterator = featureSource.getFeatures().features()) {
                while (featureIterator.hasNext()) {
                    final SimpleFeature feature = featureIterator.next();
                    onNewFeature.accept(feature);
                }
            }
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }


    public ReferencedEnvelope getBounds() throws IOException {
        return featureSource.getBounds();
    }
}
