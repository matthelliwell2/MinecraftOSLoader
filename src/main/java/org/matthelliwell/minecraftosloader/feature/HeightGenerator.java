package org.matthelliwell.minecraftosloader.feature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdge;
import com.vividsolutions.jts.triangulate.quadedge.QuadEdgeSubdivision;
import com.vividsolutions.jts.triangulate.quadedge.Vertex;
import org.matthelliwell.minecraftosloader.file.Contour;
import org.matthelliwell.minecraftosloader.file.ContourFileLoader;
import org.matthelliwell.minecraftosloader.file.SpotHeight;
import org.matthelliwell.minecraftosloader.file.SpotHeightFileLoader;

/**
 * Loads the height grid with values from the OS files and interpolation
 */
public class HeightGenerator {
    private HeightGrid heightGrid;


    /**
     * Generates a grid of heights based on the contour and spot height files
     * @param path Path to directory containing files to process
     * @param gridSquare Used to create file names to be loaded
     * @param regionNumber Which files should be processed
     * @return Object representing the heights
     */
    public HeightGrid generate(final Path path, final String gridSquare, final String regionNumber) throws IOException {

        // First load the contour file. We use this to set the bounds of the data and there we need to load it first.
        // If we are iterating through all the grid square in a national grid square then some files may not be missing,
        // eg areas in the sea won't have any data. In this case we just return null.
        final File contourFile = path.resolve(gridSquare.toUpperCase() + regionNumber + "_line.shp").toFile();
        if (!contourFile.exists()) {
            return null;
        }

        final ContourFileLoader contourFileLoader = new ContourFileLoader(contourFile, this::onNewContour);
        heightGrid = new HeightGrid(contourFileLoader.getBounds());
        contourFileLoader.processFile();

        // Now suplement the data from the contours with the spot height data. If this has large bounds than the contour data
        // then we'll just ignore any extra points
        final File spotHeightFile = path.resolve(gridSquare.toUpperCase() + regionNumber + "_point.shp").toFile();
        if ( spotHeightFile.exists() ) {
            final SpotHeightFileLoader spotHeightFileLoader = new SpotHeightFileLoader(spotHeightFile, this::onNewSpotHeight);
            spotHeightFileLoader.processFile();
        }

        // Use triangulation to set heights between the points we have
        setHeightFromTriangulation();

        // Populate any height still empty from nearest neighbour
        setHeightsFromNeighbour();

        return heightGrid;
    }


    /**
     * Uses triangulation on the contours and spot heights that we've loaded to set the heights on the grid
     */
    private void setHeightFromTriangulation() {
        final DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();

        // Filter out the unset heights as they aren't set and so shouldn't be used for triangulation
        final List<Coordinate> coords = new ArrayList<>();
        heightGrid.forEach((x, y, h) -> {
            if ( h > Integer.MIN_VALUE ) {
                coords.add(new Coordinate(x, y, h));
            }
        });

        builder.setSites(coords);

        final QuadEdgeSubdivision subdivision = builder.getSubdivision();

        // Now use the triangles to interpolate the missing grid points
        heightGrid.forEach((x, y, h) -> {
            if ( h == Integer.MIN_VALUE ) {
                final Coordinate point =  new Coordinate(x, y);
                final QuadEdge edge = subdivision.locate(point);
                double interpHeight = new Vertex(point.x, point.y).interpolateZValue(edge.orig(), edge.dest(), edge.oNext().dest());
                if ( !Double.isNaN(interpHeight)) {
                    heightGrid.setHeight(x, y, (float) interpHeight);
                }
            }
        });
    }


    private void setHeightsFromNeighbour() {
        heightGrid.forEach((x, y, h) -> {
            if ( h == Integer.MIN_VALUE ) {
                heightGrid.setHeight(x, y, getHeightOfNearestNeighbour(x, y));
            }
        });
    }

    private float getHeightOfNearestNeighbour(final int inputX, final int inputY) {
        int distance = 1;
        float height = Integer.MIN_VALUE;
        while (height == Integer.MIN_VALUE) {
            if ( heightGrid.withinBounds(inputX + distance, inputY)) {
                height = heightGrid.getHeight(inputX + distance, inputY);
            }
            if ( height == Integer.MIN_VALUE && heightGrid.withinBounds(inputX - distance, inputY)) {
                height = heightGrid.getHeight(inputX - distance, inputY);
            }
            if ( height == Integer.MIN_VALUE && heightGrid.withinBounds(inputX, inputY + distance)) {
                height = heightGrid.getHeight(inputX, inputY + distance);
            }
            if ( height == Integer.MIN_VALUE && heightGrid.withinBounds(inputX, inputY - distance)) {
                height = heightGrid.getHeight(inputX, inputY - distance);
            }

            ++distance;
        }

        return  height;
    }

    private void onNewContour(final Contour contour) {
        // Interpolate over the contour points to intermediate points
        for (final Coordinate coordinate : contour.getSmoothedContour()) {
            heightGrid.setHeight(coordinate.x, coordinate.y, contour.getHeight());
        }
    }

    private void onNewSpotHeight(final SpotHeight spotHeight) {
        heightGrid.setHeight(spotHeight.getX(), spotHeight.getY(), spotHeight.getHeight());
    }
}
