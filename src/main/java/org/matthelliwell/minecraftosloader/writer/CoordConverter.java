package org.matthelliwell.minecraftosloader.writer;

/**
 * Converts a Y coord from the OS open data into a Z coord in Minecraft and vica vera. We have to convert the coordinate
 * because in MC Z decreases as you move north but on a map Y increases as you move north. Therefore if we map Y direcly
 * to Z we get a mirror image of what we expect. To fix this we make Y negative. However we have problems we negative
 * coords in MC in this program (reason unknown but the heights are wrong at region boundaries) so after making Y
 * negative we shift it so that it is positive again.
 *
 * To make processing regions easier we make sure that 512 block boundaries in map coords also correspond to 512
 * block boundaries in MC coords.
 */
public class CoordConverter {
    private static final int OFFSET = 1_225_000;

    public static int convert(final int coord) {
        return OFFSET - coord;
    }
}
