package org.matthelliwell.util;

/**
 * A simple 2D array where the first coordinates don't have to be zero
 */
public class FloatArray2D {
    private final float[][] array;

    private final int minX;
    private final int minY;

    public FloatArray2D(final int xSize, final int ySize, final int minX, final int minY) {
        this.minX = minX;
        this.minY = minY;
        this.array = new float[xSize][ySize];
    }

    public float get(final int x, final int y) {
        return array[x - minX][y - minY];
    }

    public void set(final int x, final int y, final float value) {
        array[x - minX][y - minY] = value;
    }

    public void forEach(TriConsumer<Integer, Integer, Float> eachCell) {
        for (int x = 0; x < array.length; ++x) {
            for (int y = 0; y < array[x].length; ++y) {
                eachCell.accept(x + minX, y + minY, array[x][y]);
            }
        }
    }
}
