package org.matthelliwell.util;

/**
 * A simple 2D array where the first coordinates don't have to be zero
 */
public class ByteArray2D {
    private final byte[][] array;

    private final int minX;
    private final int minY;

    public ByteArray2D(final int xSize, final int ySize, final int minX, final int minY) {
        this.minX = minX;
        this.minY = minY;
        this.array = new byte[xSize][ySize];
    }

    public byte get(final int x, final int y) {
        return array[x - minX][y - minY];
    }

    public void set(final int x, final int y, final byte value) {
        array[x - minX][y - minY] = value;
    }
}
