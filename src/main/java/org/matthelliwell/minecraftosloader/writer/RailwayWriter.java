package org.matthelliwell.minecraftosloader.writer;

import net.morbz.minecraft.blocks.IBlock;
import net.morbz.minecraft.blocks.RailBlock;
import net.morbz.minecraft.blocks.SimpleBlock;
import net.morbz.minecraft.blocks.states.Facing2State;
import net.morbz.minecraft.blocks.states.Facing4State;
import org.matthelliwell.minecraftosloader.feature.FeatureGrid;
import org.matthelliwell.minecraftosloader.feature.HeightGrid;

public class RailwayWriter {
    private static final int EAST = 1;
    private static final int WEST = -1;

    private static final int NORTH = 1;
    private static final int SOUTH = -1;
    /** We need to look around to see what sort of railway block is needed */
    public IBlock write(final int x, final int y, final FeatureGrid featureGrid, final HeightGrid heightGrid) {
        if ( isSlopingNorth(x, y, featureGrid, heightGrid)) {
            return RailBlock.makeSloped(Facing4State.NORTH);
        } else if (isSlopingSouth(x, y, featureGrid, heightGrid)) {
            return RailBlock.makeSloped(Facing4State.SOUTH);
        } else if (isSlopingEast(x, y, featureGrid, heightGrid)) {
            return RailBlock.makeSloped(Facing4State.EAST);
        } else if (isSlopingWest(x, y, featureGrid, heightGrid)) {
            return RailBlock.makeSloped(Facing4State.WEST);
        } else if ( isNorthAndEastRailway(x, y, featureGrid)) {
            return RailBlock.makeCurved(RailBlock.RailCurve.NORTH_EAST);
        } else if ( isNorthAndWestRailway(x, y, featureGrid)) {
            return RailBlock.makeCurved(RailBlock.RailCurve.NORTH_WEST);
        } else if ( isSouthAndEastRailway(x, y, featureGrid)) {
            return RailBlock.makeCurved(RailBlock.RailCurve.SOUTH_EAST);
        } else if ( isSouthAndWestRailway(x, y, featureGrid)) {
            return RailBlock.makeCurved(RailBlock.RailCurve.SOUTH_WEST);
        } else if ( isNorthSouthRailway(x, y, featureGrid)) {
            return RailBlock.makeStraight(Facing2State.NORTH_SOUTH);
        } else if ( isEastWestRailway(x, y, featureGrid)) {
            return RailBlock.makeStraight(Facing2State.EAST_WEST);
        }

        System.out.println("Unable to calculate railway type at " + x + ", " + y);
        return SimpleBlock.GRASS;

    }

    private boolean isNorthSouthRailway(final int x, final int y, final FeatureGrid featureGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + NORTH) == FeatureGrid.RAILWAY ||
                featureGrid.getFeatureWithBoundsCheck(x, y + SOUTH) == FeatureGrid.RAILWAY;
    }

    private boolean isEastWestRailway(final int x, final int y, final FeatureGrid featureGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x + EAST, y) == FeatureGrid.RAILWAY ||
                featureGrid.getFeatureWithBoundsCheck(x + WEST, y) == FeatureGrid.RAILWAY;
    }

    private boolean isSouthAndEastRailway(final int x, final int y, final FeatureGrid featureGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + SOUTH) == FeatureGrid.RAILWAY &&
                featureGrid.getFeatureWithBoundsCheck(x + EAST, y) == FeatureGrid.RAILWAY;
    }

    private boolean isSouthAndWestRailway(final int x, final int y, final FeatureGrid featureGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + SOUTH) == FeatureGrid.RAILWAY &&
                featureGrid.getFeatureWithBoundsCheck(x + WEST, y) == FeatureGrid.RAILWAY;
    }

    private boolean isNorthAndWestRailway(final int x, final int y, final FeatureGrid featureGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + NORTH) == FeatureGrid.RAILWAY &&
                featureGrid.getFeatureWithBoundsCheck(x + WEST, y) == FeatureGrid.RAILWAY;
    }

    private boolean isNorthAndEastRailway(final int x, final int y, final FeatureGrid featureGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + NORTH) == FeatureGrid.RAILWAY &&
                featureGrid.getFeatureWithBoundsCheck(x + EAST, y) == FeatureGrid.RAILWAY;
    }

    private boolean isSlopingNorth(final int x, final int y, final FeatureGrid featureGrid, final HeightGrid heightGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + NORTH) == FeatureGrid.RAILWAY &&
                (int)heightGrid.getHeight(x, y) < (int)heightGrid.getHeightWithBounds(x, y + NORTH);
    }

    private boolean isSlopingSouth(final int x, final int y, final FeatureGrid featureGrid, final HeightGrid heightGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x, y + SOUTH) == FeatureGrid.RAILWAY &&
                (int)heightGrid.getHeight(x, y) < (int)heightGrid.getHeightWithBounds(x, y + SOUTH);
    }

    private boolean isSlopingEast(final int x, final int y, final FeatureGrid featureGrid, final HeightGrid heightGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x + EAST, y) == FeatureGrid.RAILWAY &&
                (int)heightGrid.getHeight(x, y) < (int)heightGrid.getHeightWithBounds(x + EAST, y);
    }

    private boolean isSlopingWest(final int x, final int y, final FeatureGrid featureGrid, final HeightGrid heightGrid) {
        return featureGrid.getFeatureWithBoundsCheck(x + WEST, y) == FeatureGrid.RAILWAY &&
                (int)heightGrid.getHeight(x, y) < (int)heightGrid.getHeightWithBounds(x + WEST, y);
    }
}
