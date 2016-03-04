package org.matthelliwell.minecraftosloader.writer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.morbz.minecraft.blocks.FlowerBlock;
import net.morbz.minecraft.blocks.IBlock;
import net.morbz.minecraft.blocks.SandBlock;
import net.morbz.minecraft.blocks.SandstoneBlock;
import net.morbz.minecraft.blocks.SaplingBlock;
import net.morbz.minecraft.blocks.SimpleBlock;
import net.morbz.minecraft.blocks.StainedBlock;
import net.morbz.minecraft.world.World;
import org.matthelliwell.minecraftosloader.feature.BuildingGenerator;
import org.matthelliwell.minecraftosloader.feature.FeatureGrid;
import org.matthelliwell.minecraftosloader.feature.ForeshoreGenerator;
import org.matthelliwell.minecraftosloader.feature.FunctionalSiteGenerator;
import org.matthelliwell.minecraftosloader.feature.GlassHouseGenerator;
import org.matthelliwell.minecraftosloader.feature.HeightGenerator;
import org.matthelliwell.minecraftosloader.feature.HeightGrid;
import org.matthelliwell.minecraftosloader.feature.ImportantBuildingGenerator;
import org.matthelliwell.minecraftosloader.feature.LakeGenerator;
import org.matthelliwell.minecraftosloader.feature.RailwayGenerator;
import org.matthelliwell.minecraftosloader.feature.RoadGenerator;
import org.matthelliwell.minecraftosloader.feature.StreamGenerator;
import org.matthelliwell.minecraftosloader.feature.TidalWaterGenerator;
import org.matthelliwell.minecraftosloader.feature.WoodlandGenerator;

/**
 * Writes the data for a 10x10km square within a national grid square
 */
class GridSquareWriter {
    private static final int MAX_GROUND_DEPTH = 20;
    private final RailwayWriter railwayWriter = new RailwayWriter();

    public void write(final World world,
                      final Path localDataPath,
                      final Path terrainDataPath,
                      final String nationalGridReferenceSquare,
                      final String gridSquareNumber,
                      final long[] blockCount) throws IOException {

        System.out.println("Writing square " + nationalGridReferenceSquare + " " + gridSquareNumber);
        // Process the heigh related features as we need the height grid before we can do much else
        final HeightGrid heightGrid = new HeightGenerator().generate(terrainDataPath, nationalGridReferenceSquare, gridSquareNumber);
        if ( heightGrid == null ) {
            return;
        }

        // Create the feature grid into which we'll do the features on the terrain
        final FeatureGrid featureGrid = new FeatureGrid(heightGrid.getBounds());

        // Load functional sites before buildings as building may be inside these so don't want to overwrite the building
        new FunctionalSiteGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);

        new RoadGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new ImportantBuildingGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new BuildingGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new WoodlandGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new LakeGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new StreamGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new GlassHouseGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new TidalWaterGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new ForeshoreGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);
        new RailwayGenerator(featureGrid).generate(localDataPath, nationalGridReferenceSquare);

        final HeightScaler scaler = new HeightScaler(heightGrid.getMinHeight(), heightGrid.getMaxHeight());

        heightGrid.forEachRegionInParallel((x, y, h) -> {
                    final int scaledHeight = Math.round(scaler.scale(h));
                    if (scaledHeight > 0) {
                        final List<IBlock> blocks = setBlocksForColumn(x, y, scaledHeight, featureGrid, heightGrid, blockCount);

                        world.setBlocks(x, CoordConverter.convert(y), blocks.toArray(new IBlock[blocks.size()]));
                    }
                },
                (x, y) -> world.calculateSkylightForRegion(x, CoordConverter.convert(y)));

        System.out.println("Scaling height by " + scaler.getScale() * 100 + "%");
    }

    private List<IBlock> setBlocksForColumn(final int x, final int y, final int height, final FeatureGrid featureGrid, final HeightGrid heightGrid, final long[] blockCount) {
        final List<IBlock> blocks = new ArrayList<>(height + 2);
        blocks.add(SimpleBlock.BEDROCK);

        for ( int h = 1; h < height - MAX_GROUND_DEPTH; ++h ) {
            blocks.add(SimpleBlock.AIR);
        }

        for ( int h = height - MAX_GROUND_DEPTH; h < height; ++h ) {
            if (h > 0) {
                blocks.add(SimpleBlock.COBBLESTONE);
            }
        }

        switch ( featureGrid.getFeature(x, y) ) {
            case FeatureGrid.GRASS:
                blocks.add(SimpleBlock.GRASS);
                break;
            case FeatureGrid.OAK:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(SaplingBlock.OAK_SAPLING);
                break;
            case FeatureGrid.BIRCH:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(SaplingBlock.BIRCH_SAPLING);
                break;
            case FeatureGrid.DANELION:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(FlowerBlock.DANDELION);
                break;
            case FeatureGrid.POPPY:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(FlowerBlock.POPPY);
                break;
            case FeatureGrid.OXEYE_DAISY:
                blocks.add(SimpleBlock.GRASS);
                blocks.add(FlowerBlock.OXEYE_DAISY);
                break;
            case FeatureGrid.LAKE:
                blocks.add(SimpleBlock.WATER);
                break;
            case FeatureGrid.BUILDING:
                // We don't know how high building are so we'll just do them a few blocks high in a uniform colour
                // on a cobblestone foundation
                blocks.add(SimpleBlock.COBBLESTONE);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.BRICK_BLOCK);
                blocks.add(SimpleBlock.HARDENED_CLAY);
                break;
            case FeatureGrid.FUNCTIONAL_SITE:
                // Pave the site in cobblestones
                blocks.add(SimpleBlock.COBBLESTONE);
                break;
            case FeatureGrid.IMPORTANT_BUILDING:
                // Build these in sandstone and higher than normal buildings
                blocks.add(SimpleBlock.COBBLESTONE);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.CHISELED);
                blocks.add(SandstoneBlock.SMOOTH);
                break;
            case FeatureGrid.ROAD:
                // Pave the site in clay
                blocks.add(new StainedBlock(StainedBlock.StainedMaterial.CLAY, StainedBlock.StainedColor.GRAY));
                break;
            case FeatureGrid.GLASSHOUSE:
                blocks.add(SimpleBlock.COBBLESTONE);
                blocks.add(SimpleBlock.GLASS);
                blocks.add(SimpleBlock.GLASS);
                blocks.add(SimpleBlock.GLASS);
                break;
            case FeatureGrid.TIDAL_WATER:
                blocks.add(SimpleBlock.WATER);
                break;
            case FeatureGrid.FORESHORE:
                blocks.add(SandBlock.SAND);
                break;
            case FeatureGrid.RAILWAY:
                blocks.add(SimpleBlock.GRAVEL);
                blocks.add(railwayWriter.write(x, y, featureGrid, heightGrid));
                break;
            default:
                System.out.println("Unknown feature " + featureGrid.getFeature(x, y));
                break;

        }

        blockCount[0] += blocks.size();
        return blocks;
    }


}
